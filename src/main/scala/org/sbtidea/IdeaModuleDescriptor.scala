package org.sbtidea

/**
 * Copyright (C) 2010, Mikko Peltonen, Jon-Anders Teigen, Michal Příhoda, Graham Tackley, Ismael Juma, Odd Möller, Johannes Rudolph
 * Licensed under the new BSD License.
 * See the LICENSE file for details.
 */

import sbt._
import java.io.File

import xml.{UnprefixedAttribute, Node, Text}


class IdeaModuleDescriptor(val imlDir: File, projectRoot: File, val project: SubProjectInfo, val env: IdeaProjectEnvironment, val userEnv: IdeaUserEnvironment, val log: Logger, scalaVersion: String, scalaFacet: Boolean = true, includeGeneratedClasses: Boolean = false) extends SaveableXml {
  val path = String.format("%s/%s.iml", imlDir.getAbsolutePath, project.name)

  def relativePath(file: File) = {
    IO.relativize(projectRoot, file.getCanonicalFile).map ("$MODULE_DIR$/../" + _).getOrElse(file.getCanonicalPath)
  }
  val s = java.io.File.separator
  val sources = project.compileDirs.sources.map(relativePath(_))
  val resources = project.compileDirs.resources.map(relativePath(_))
  val testSources = project.testDirs.sources.map(relativePath(_))
  val testResources = project.testDirs.resources.map(relativePath(_))
  lazy val generatedReference = "file://$MODULE_DIR$/../target"+ s + "scala-"+scalaVersion + s + "classes_managed"
  val genarated = if(includeGeneratedClasses) {
    <orderEntry type="module-library">
        <library>
          <CLASSES>
            <root url={generatedReference} />
          </CLASSES>
          <JAVADOC />
          <SOURCES />
        </library>
      </orderEntry>
  } else scala.xml.Null
  val facet = if (scalaFacet) {
     <facet type="scala" name="Scala">
          <configuration>
            {
              project.basePackage.map(bp => <option name="basePackage" value={bp} />).getOrElse(scala.xml.Null)
            }
            <option name="compilerLibraryLevel" value="Project" />
            <option name="compilerLibraryName" value={ "scala-" + project.scalaInstance.version } />
            {
              if (env.useProjectFsc) <option name="fsc" value="true" />
            }
            {
              if (project.scalacOptions.contains("-deprecation")) <option name="deprecationWarnings" value="true" />
            }
            {
              if (project.scalacOptions.contains("-unchecked")) <option name="uncheckedWarnings" value="true" />
            }
            <option name="compilerOptions" value={ project.scalacOptions.mkString(" ") } />
          </configuration>
        </facet>
        } else scala.xml.Null
        
  def content: Node = {
    <module type="JAVA_MODULE" version="4">
      <component name="FacetManager">
        {facet}
        { if (project.webAppPath.isDefined && userEnv.webFacet == true) webFacet() else scala.xml.Null }
        { project.extraFacets }
      </component>
      <component name="NewModuleRootManager" inherit-compiler-output={env.projectOutputPath.isDefined.toString}>
        {
          if (env.projectOutputPath.isEmpty) {
            <output url={"file://" + relativePath(project.compileDirs.outDir)} />
            <output-test url={"file://" + relativePath(project.testDirs.outDir)} />
          } else scala.xml.Null
        }
        <exclude-output />
        <content url={"file://" + relativePath(project.baseDir) }>
          { sources.map(sourceFolder(_, false, project.packagePrefix)) }
          { resources.map(sourceFolder(_, false, project.packagePrefix)) }
          { testSources.map(sourceFolder(_, true, project.packagePrefix)) }
          { testResources.map(sourceFolder(_, true, project.packagePrefix)) }
          {

            def dontExcludeManagedSources(toExclude:File):Seq[File] = {

              def isParent(f:File):Boolean = {
                f == toExclude || (f != null && isParent(f.getParentFile))
              }

              val managed = project.compileDirs.sources ++ project.testDirs.sources
              val dontExclude = managed.exists(isParent)

              if(dontExclude)
                toExclude.listFiles().toSeq.filter(_.isDirectory).filterNot(managed.contains).flatMap(dontExcludeManagedSources)
              else
                Seq(toExclude)
            }

            env.excludedFolders.split(",").toList.map(_.trim)
              .map(entry => new File(project.baseDir, entry))
              .flatMap(dontExcludeManagedSources)
              .sortBy(_.getName).map { exclude =>
              log.info(String.format("Excluding folder %s\n", exclude))
              <excludeFolder url={String.format("file://%s", relativePath(exclude))} />
            }
          }
        </content>
        {
          /*project match {
            case sp: ScalaPaths if ! env.compileWithIdea.value =>
              val nodeBuffer = new xml.NodeBuffer
              if (sp.testResources.getFiles.exists(_.exists))
                nodeBuffer &+ moduleLibrary(Some("TEST"), None, None,
                  Some("file://$MODULE_DIR$/" + relativePath(sp.testResourcesOutputPath.asFile)), false)
              if (sp.mainResources.getFiles.exists(_.exists))
                nodeBuffer &+ moduleLibrary(None, None, None,
                  Some("file://$MODULE_DIR$/" + relativePath(sp.mainResourcesOutputPath.asFile)), false)
              nodeBuffer
            case _ => xml.Null
          }*/ xml.Null
        }
        <orderEntry type="inheritedJdk"/>
        <orderEntry type="sourceFolder" forTests="false"/>
        {
        // what about j.extraAttributes.get("e:docUrl")?
        project.libraries.map(ref => {
          val orderEntry = <orderEntry type="library" name={ ref.library.name } level="project"/>
          ref.config match {
                case IdeaLibrary.CompileScope => orderEntry
                case scope => orderEntry % new UnprefixedAttribute("scope", scope.configName, scala.xml.Null)
          }
        })
        }

        {
          //FIXME Take dependency scope into account
          project.dependencyProjects.distinct.map { name =>
            log.debug("Project dependency: " + name)
            <orderEntry type="module" module-name={name} exported=""/>
          }
        }
        {genarated}
        {
          project.classpathDeps.map { case (classesDir, sourceDirs) =>
            <orderEntry type="module-library">
              <library>
                <CLASSES>
                  <root url={ "file://%s".format(classesDir.getAbsolutePath) } />
                </CLASSES>
                <JAVADOC />
                <SOURCES>
                {
                  sourceDirs.filter(_.exists).map { srcDir =>
                    <root url={ "file://%s".format(srcDir.getAbsolutePath) } />
                  }
                }
                </SOURCES>
              </library>
            </orderEntry>
          }
        }
      </component>
    </module>
  }

  def sourceFolder(path: String, isTestSourceFolder: Boolean,
                   packagePrefix: Option[String]) = {
    val pkg = packagePrefix.map(Text(_))
    <sourceFolder url={"file://" + path}
                  isTestSource={isTestSourceFolder.toString}
                  packagePrefix={pkg} />
  }

  def webFacet(): Node = {
    <facet type="web" name="Web">
      <configuration>
        <descriptors>
          <deploymentDescriptor name="web.xml" url={String.format("file://%s/WEB-INF/web.xml", relativePath(project.webAppPath.get))} />
        </descriptors>
        <webroots>
          <root url={String.format("file://%s", relativePath(project.webAppPath.get))} relative="/" />
        </webroots>
      </configuration>
    </facet>
  }
}
