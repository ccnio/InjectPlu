package com.ccnio

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.ide.common.internal.WaitableExecutor
import com.android.utils.FileUtils
import com.ccnio.utils.Log
import org.gradle.api.Project

import java.util.concurrent.Callable

/**
 * Transform两个基础概念:TransformInput,TransformOutputProvider
 *
 * TransformInput,包括：DirectoryInput集合,是指以源码的方式参与项目编译的所有目录结构及其目录下的源码文件;
 * JarInput集合是指以jar包方式参与项目编译的所有本地jar包和远程jar包（此处的jar包包括aar）
 *
 * TransformOutputProvider,Transform的输出，通过它可以获取到输出路径等信息
 *
 * Transform优化
 * 一般就三种：
 * 增量编译
 * 并发编译
 * include... exclude...缩小transform范围
 *
 * 自定义 Transform 无法处理 Dex ；
 */
class PluTransform extends Transform {
    Project project
    private WaitableExecutor mWaitableExecutor = WaitableExecutor.useGlobalSharedThreadPool()//CountDownLatch

    @Override
    void transform(TransformInvocation transformInvocation) {
        boolean isIncremental = transformInvocation.isIncremental()
        Log.d("transform isIncremental = $isIncremental")
        TransformOutputProvider outputProvider = transformInvocation.getOutputProvider();
        if (!isIncremental) {
            outputProvider.deleteAll()
        }

        transformInvocation.inputs.each { TransformInput input ->
            // 处理Jar
            input.jarInputs.each { JarInput jarInput ->
                mWaitableExecutor.execute(new Callable<Object>() {
                    @Override
                    Object call() throws Exception {
                        File outputJar = outputProvider.getContentLocation(jarInput.getName(), jarInput.getContentTypes(), jarInput.getScopes(), Format.JAR)
                        Log.d("jars: input = $jarInput; output = $outputJar")

                        if (!isIncremental) {
                            transformJar(jarInput, outputJar)
                        } else {
                            switch (jarInput.status) {
                                case Status.NOTCHANGED:
                                    Log.d("NOTCHANGED")
                                    break
                                case Status.ADDED: //fall through
                                case Status.CHANGED:
                                    if (status == Status.CHANGED) {
                                        //Changed的状态需要先删除之前的
                                        FileUtils.delete(outputJar)
                                        Log.d("CHANGED")
                                    } else {
                                        Log.d("ADDED")
                                    }
                                    transformJar(jarInput, outputJar)
                                    break
                                case Status.REMOVED:
                                    Log.d("REMOVED")
                                    FileUtils.delete(outputJar)
                                    break
                            }
                        }
                        return null
                    }
                })
            }

            // 处理源码文件
            input.directoryInputs.each { DirectoryInput dirInput ->
                mWaitableExecutor.execute(new Callable<Object>() {
                    @Override
                    Object call() throws Exception {
                        File outputDir = outputProvider.getContentLocation(dirInput.getName(), dirInput.getContentTypes(), dirInput.getScopes(), Format.DIRECTORY);
                        Log.d("dirs: input = $dirInput; output = $outputDir")
                        FileUtils.mkdirs(outputDir)

                        if (!isIncremental) {
                            transformDir(dirInput, outputDir)
                        } else {
                            String inputDirPath = dirInput.getFile().getAbsolutePath()
                            String outputDirPath = outputDir.getAbsolutePath()
                            Map<File, Status> fileStatusMap = dirInput.getChangedFiles()
                            fileStatusMap.each { Map.Entry<File, Status> entry ->
                                File inputFile = entry.getKey()
                                Status status = entry.getValue()
                                String destFilePath = inputFile.getAbsolutePath().replace(inputDirPath, outputDirPath)
                                File destFile = new File(destFilePath)
                                Log.d("dir fileStatusMap: inputFile = $inputFile; destFile = $destFile")
                                switch (status) {
                                    case Status.NOTCHANGED:
                                        Log.d("NOTCHANGED")
                                        break
                                    case Status.REMOVED:
                                        Log.d("REMOVED")
                                        FileUtils.delete(destFile)
                                        break
                                    case Status.ADDED://fall through
                                    case Status.CHANGED:
                                        Log.d(status == Status.ADDED ? "ADDED" : "CHANGED")
                                        FileUtils.copyFile(inputFile, destFile)//overwritten
                                        break
                                }
                            }
                        }
                        return null
                    }
                })

            }
        }
        //等待所有任务结束
        mWaitableExecutor.waitForTasksWithQuickFail(true)
    }

    void transformJar(JarInput jarInput, File output) {
        //do some transform
        //将修改过的字节码copy到dest，就可以实现编译期间干预字节码的目的了
        FileUtils.copyFile(jarInput.getFile(), output)
    }

    void transformDir(DirectoryInput directoryInput, File output) {
        //do some transform
        //将修改过的字节码copy到dest，就可以实现编译期间干预字节码的目的了
        FileUtils.copyDirectory(directoryInput.getFile(), output)
    }

    /**
     * 作用范围，常用类型：
     * EXTERNAL_LIBRARIES ：外部库 Only the external libraries
     * PROJECT ： 只有项目内容 Only the project (module) content
     * PROVIDED_ONLY ： 只提供本地或远程依赖项 Local or remote dependencies that are provided-only
     * SUB_PROJECTS ： 只有子项目 Only the sub-projects (other modules)
     */
    @Override
    Set<QualifiedContent.Scope> getScopes() {

        /**
         * 只在主module引用插件
         * SCOPE_FULL_PROJECT 只在主module（build.gradle中含有com.android.application插件）引用即可。这里处理的模块包括
         * 本模块，子模块以及第三方jar包，这样我们就能在主模块中处理所有的class文件了，可见我们是可以只在主模块中引入的，这样做的话，所有子模块会以jar包的形式作为输入。
         *
         * 在每个module中都引入该如何做呢？
         * 首先是注册方式要修改：
         *  def extension = project.getExtensions().findByType(AppExtension.class)
         *  def isForApplication = true
         *  if (extension == null) {//说明当前使用在library中
         *     extension = project.getExtensions().findByType(LibraryExtension.class)
         *     isForApplication = false
         *}*  extension.registerTransform(new PluTransform(project,isForApplication))
         *  关键是我们在Transform中要记录当前是应用于主模块还是子模块了。这种模式下，每一个模块都会执行自己的transform()方法，
         *  所以这里的getScopes()方法要做些修改：
         *         def scopes = new HashSet()
         *         scopes.add(QualifiedContent.Scope.PROJECT)
         *         if (isForApplication) {*             //application module中加入此项可以处理第三方jar包
         *             scopes.add(QualifiedContent.Scope.EXTERNAL_LIBRARIES)
         *}*         return scopes
         * 这里对于主模块的情况下应该额外处理第三方jar包，子模块只要处理自己的项目代码即可。
         * 其实经过实验，所有子模块的依赖的第三方jar包只会在处理主模块中输入，换句话说子模块是永远不可能处理第三方jar包的。
         *
         * gradle插件到底是应该只在主module中引入还是再所有的module中都引入。
         * 衡量的关键点就是编译速度，如果只在主module中引入的话，子module其实是以jar包的形式作为输入文件来 处理的，
         * 这样我们就算只修改了子module中一个文件，我们都需要将整个jar解压，然后处理该jar中的所有class文件，最后还得压缩一次， 多做了无用功；
         * 如果我们放在所有的module中引入的话，针对这种情况我们只需要处理改动的class文件即可，能节省很多时间，所以我推荐放到所有module引入。
         *
         */
        return TransformManager.SCOPE_FULL_PROJECT
//等同ImmutableSet.of(Scope.PROJECT, Scope.SUB_PROJECTS, Scope.EXTERNAL_LIBRARIES);
    }
    /**
    
     gradle插件应该在application模块引入还是library模块引入？
目前，我们的插件都是直接在application模块中引入的，那么多模块情况下怎么办？每个模块都要引入吗？可以只在主模块引入吗？应该只在主模块引入吗？

4.1.1 只在主模块引入
我们知道，butterknife是需要在每个模块都引入的，其实，对于多模块来说，我们完全可以只在application主模块中引入插件，这里要注意Transform中的getScopes()方法：

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        //此次是只允许在主module（build.gradle中含有com.android.application插件）
        //所以我们需要修改所有的module
        return TransformManager.SCOPE_FULL_PROJECT

    }

这里的SCOPE_FULL_PROJECT其实是这样的：

        SCOPE_FULL_PROJECT = Sets.immutableEnumSet(Scope.PROJECT, new Scope[]{Scope.SUB_PROJECTS, Scope.EXTERNAL_LIBRARIES});

说明这里处理的模块包括本模块，子模块以及第三方jar包，这样我们就能在主模块中处理所有的class文件了，可见我们是可以只在主模块中引入的，这样做的话，所有子模块会以jar包的形式作为输入。

4.1.2 在每个模块都引入
那么如果想要在每个module中都引入该如何做呢？
首先是注册方式要修改：

    @Override
    void apply(Project project) {
        project.getExtensions()
                .create("methodTrace", MethodTraceExtension.class)
        def extension = project.getExtensions().findByType(AppExtension.class)
        def isForApplication = true
        if (extension == null) {
            //说明当前使用在library中
            extension = project.getExtensions().findByType(LibraryExtension.class)
            isForApplication = false
        }
        extension.registerTransform(new MethodTraceTransform(project,isForApplication))

    }

关键是我们在Transform中要记录当前是应用于主模块还是子模块了。
这种模式下，每一个模块都会执行自己的transform()方法，所以这里的getScopes()方法要做些修改：

 @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        def scopes = new HashSet()
        scopes.add(QualifiedContent.Scope.PROJECT)
        if (isForApplication) {
            //application module中加入此项可以处理第三方jar包
            scopes.add(QualifiedContent.Scope.EXTERNAL_LIBRARIES)
        }
        return scopes
    }
这里对于主模块的情况下应该额外处理第三方jar包，子模块只要处理自己的项目代码即可。
其实进过实验，所有子模块的依赖的第三方jar包只会在处理主模块中输入，换句话说子模块是永远不可能处理第三方jar包的。
    
    **/
    
    

    /**
     * 是否支持增量编译，返回true的话表示支持，这个时候可以获取Input状态
     * JarInput通过getStatus()来获取，包含了NOTCHANGED、ADDED、CHANGED、REMOVED，所以可以根据JarInput的status来对它进行相应的处理，比如添加或者移除。
     * DirectoryInput通过getChangedFiles()获取一个Map<File, Status>集合，然后根据File对应的Status来对File进行处理。
     *
     * NOTCHANGED 当前文件不需要处理，甚至复制操作都不用
     * ADDED、CHANGED 正常处理，输出给下一个任务
     * REMOVED 移除outputProvider获取路径对应的文件
     * 如果不支持增量编译，就在处理.class之前把之前的输出目录中的文件删除。
     */
    @Override
    boolean isIncremental() {
        return true
    }

    /**
     * 需要处理的数据类型，must be of type {@link QualifiedContent.DefaultContentType}:共两种
     * CLASSES: compiled Java code. This can be in a Jar file or in a folder.
     * RESOURCES：standard Java resources.
     */
    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    String getName() {
        return "InjectPlu"
    }


    // 除了主输入/输出流之外，Transform还可以额外定义另外的流供下个使用，不过我们平时用到的不多，
    // 可以根据系统自带的Transform源码看看它输出了啥，比如ProguardTransform,CustomClassTransform
    //   @Override   getSecondaryFiles

    PluTransform(Project project) {
        this.project = project
    }
}
