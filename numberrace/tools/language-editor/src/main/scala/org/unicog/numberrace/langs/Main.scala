package org.unicog.numberrace.langs

object Main {

  object editorURLClassLoader extends java.net.URLClassLoader(Array(Main.getClass.getProtectionDomain()
    .getCodeSource().getLocation()), Thread.currentThread.getContextClassLoader) {

    override protected def findClass(name: String) = {
      //  print("Looking for class :")
      //  print(name)
      //  println
      val c = super.findClass(name)
      c
    }

    def add(url: java.net.URL): Unit = {
      println(url)
      //              println
      addURL(url)
    }

    // MacOSX Change BEGIN
    // this is needed for us to find the innermost caller for our org.apache.* compatability classpath hack
    val childmostCaller = new ThreadLocal[ClassLoader];
    // MacOSX Change END

    lazy val parent = getParent

    @throws(classOf[ClassNotFoundException])
    override protected def loadClass(name: String, resolve: Boolean): Class[_] = {
      // First, check if the class has already been loaded
      var c = findLoadedClass(name)
      if (null == c) {
        // MacOSX Change BEGIN
        var isChildmost = false;
        if (childmostCaller.get() == null) {
          isChildmost = true;
          childmostCaller.set(this);
        }
        // MacOSX Change END
        try {
          c = findClass(name)
        } catch {
          case e: ClassNotFoundException =>
            // If still not found, then invoke findClass in order
            // to find the class.
            // MacOSX Change BEGIN
            c = parent.loadClass(name);
        } finally {
          if (isChildmost)
            childmostCaller.remove()
        }
        if (resolve) {
          resolveClass(c)
        }
      }
      c
    }
  }

  import scala.swing._
  import FileChooser.Result._
  import FileChooser.SelectionMode._
  import javax.swing.filechooser.FileFilter
  import java.io.File

  lazy val lib_dir = lib(new scala.swing.BorderPanel)

  def lib(cmp: Component): File = {
    val prefs = java.util.prefs.Preferences.userRoot.node("org.unicog.numberrace")

    val fc = prefs.get("installation_dir", null) match {
      case null => new FileChooser(new File(System.getProperty("user.home")))
      case inst_dir => new FileChooser(new File(inst_dir))
    }

    fc.title = "Select your [NumberRace] installation folder"
    fc.fileSelectionMode = DirectoriesOnly
    fc.fileFilter = new FileFilter {
      override def accept(f: File): Boolean = f.isDirectory
      override def getDescription: String = "Folders only"
    }

    fc.showDialog(cmp, "Use It") match {
      case Approve =>
        val inst_folder = fc.selectedFile
        var lib: File = null
        if (System.getProperty("os.name").toLowerCase().startsWith("mac os x"))
          lib = new File(inst_folder + "/Contents/Resources/Java")
        else
          lib = new File(inst_folder + File.separator + "lib")
        if (lib.exists) {
          prefs.put("lib_dir", lib.getAbsolutePath)
          prefs.put("installation_dir", inst_folder.getAbsolutePath)
          lib
        } else {
          null
        }
      case _ =>
        null
    }
  }

  var core_path: String = null

  def main(args: Array[String]): Unit = {

    java.lang.Thread.currentThread.setContextClassLoader(editorURLClassLoader)

    javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
      override def run {
        try {
          import javax.swing.UIManager

          UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel")

        } catch { case e: Exception => }
      }
    });

    if (lib_dir != null) {
      val jarFiles = lib_dir.listFiles(new java.io.FilenameFilter() {
        override def accept(dir: File, name: String) = {
          name.endsWith(".jar")
        }
      });

      jarFiles.foreach((f: File) => {
        if (f.getName.startsWith("numberrace-core")) {
          core_path = f.getPath
        }
        editorURLClassLoader.add(f.toURI.toURL)
      })

      val editorMain = editorURLClassLoader.loadClass("org.unicog.numberrace.langs.editor.EditorApp")
      editorMain.getMethod("start", Array(classOf[String]): _*).invoke(null, Array(core_path): _*)
    } else {
      print("Connot find needed jars. Most probably because you have selected wrong folder.\n")
    }

  }

}
