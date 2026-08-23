package org.unicog.numberrace.langs.editor

object Utils {

//  case class javaenumeraitoniterator[a](enum : java.util.enumeration[a]) extends iterator[a] {
//    def hasnext = enum.hasmoreelements
//    def next = enum.nextelement
//  } 
//                             
//  case class javaenumerationiterable[a](enum : java.util.enumeration[a]) extends iterable[a] {
//    def elements = javaenumeraitoniterator(enum)
//  }
//                             
//  implicit def enumerationitarable[a](enum : java.util.enumeration[a]) : iterable[a] = javaenumerationiterable(enum)
//  
  import java.io.{FileInputStream, FileOutputStream, IOException, File}

  private[editor] def ensureParentExists(file : File) = {
        if(!file.getParentFile.exists) {
          file.getParentFile.mkdirs
        }
  }
  private[editor] def copyFile(src : File, dst : File) = {
    if(dst.isDirectory) throw new RuntimeException("FileCopy: destination can not be Directory")
    
    if(!src.equals(dst)) {
        ensureParentExists(dst)
        try {
            // Create channel on the source
            val srcChannel = new FileInputStream(src).getChannel();
        
            // Create channel on the destination
            val dstChannel = new FileOutputStream(dst).getChannel();
        
            // Copy file contents from source to destination
            dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
        
            // Close the channels
            srcChannel.close();
            dstChannel.close();
            true
        } catch {
          case e : IOException => e.printStackTrace; false
        }
    } else {
      println("Src and destination are the same - doing nothing")
    }
  }
  
  import java.util.jar._
  import java.io.BufferedOutputStream
  import java.io.BufferedInputStream
  
  private[editor] def createJar(jarFile : File, src : File) : File = {
    
    jarFile.delete
    jarFile.createNewFile
    
    val output = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(jarFile)));

    jar(output, src, src.getAbsolutePath.length + 1)
    
    output.flush
    output.close
    
    jarFile
  }

  private lazy val readBuff = new Array[Byte](8192)
  
  private def jar(output : JarOutputStream, src : File, rootPathLen : Int) {
    
    if(!src.isDirectory) {
      val input = new BufferedInputStream(new FileInputStream(src))
      
      val entry = new JarEntry(src.getAbsolutePath.substring(rootPathLen).replace(File.separatorChar, '/'))
      
      output.putNextEntry(entry)
      var len = 0
      while({ len = input.read(readBuff); len != -1}) {
        output.write(readBuff,0,len)
      }
      output.closeEntry  
      printf("\nAdded [%s] : [%d] -> [%d]", entry, entry.getSize ,entry.getCompressedSize)
    } else
      src.listFiles.foreach(jar(output, _, rootPathLen))  
  }
  
/*
public static byte[] createZip(File[] classes, File rootDir) {
        byte buffer[] = new byte[512];
        ByteArrayOutputStream os = new ByteArrayOutputStream(2000);

        ZipOutputStream out = new ZipOutputStream(os);
        out.setMethod(ZipOutputStream.DEFLATED);
        int rootPathLen = rootDir.getAbsolutePath().length() + 1;
        StringBuilder sb = new StringBuilder("Archiving classes from files :");
        for (int i = 0; i < classes.length; i++) {
            File file = classes[i];

            String sFileName = file.getAbsolutePath().substring(rootPathLen);
            try {
                String sFullName = sFileName.replace(File.separatorChar, '/');
                sb.append("\nAdding file to ZIPARC : ").append(sFullName);
                InputStream in = new FileInputStream(file);
                ZipEntry entry = new ZipEntry(sFullName);
                out.putNextEntry(entry);
                int len = 0;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
                out.closeEntry();

                sb.append("\n\tnormal : ").append(entry.getSize());
                sb.append("\n\tcompressed :  ").append(entry.getCompressedSize());

            } catch (java.lang.Exception ex) {
                sb.append("\n-- Something wrong during wtiting classfile --");
                ex.printStackTrace();
            }
        }

        log.fine(sb.toString());
        
        try {
            os.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return os.toByteArray();
    }
*/

}
