import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

public class ProcessPlist
{
  public static void main(String[] args)
  {
    if (args.length < 1)
    {
      System.err.println("参数错误");
      return;
    }
    String srcDir = args[0];
    File srcFileDir = new File(srcDir);
    if (!srcFileDir.isDirectory())
    {
      System.err.println(srcDir + "不正确，请确认是目录!");
      return;
    }
    
    List<String> fileList = new ArrayList();     
    listFile(fileList,srcFileDir);
    
    for(String _name:fileList) {
    	String png =  _name + ".png";
    	String plist =  _name + ".plist";
    	
        System.err.println("fileList:" + _name);
        
        try
        {
          Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("png");
          ImageReader reader = (ImageReader)readers.next();
          
          InputStream source = new FileInputStream(png);
          ImageInputStream iis = ImageIO.createImageInputStream(source);
          reader.setInput(iis, true);
          
          File plistFile = new File(plist);

          List<RectSize> list = testXML(plistFile.getAbsolutePath());
          for (Iterator<RectSize> it = list.iterator(); it.hasNext();)
          {
            RectSize rectSize = (RectSize)it.next();
            
            String lpng = _name + File.separatorChar + rectSize.name;
            String Path = lpng.substring(0, lpng.lastIndexOf("/"));
            
            System.err.println(lpng);
            System.err.println(Path);
            
            File ldir = new File(Path);
            if(!ldir.exists()) {
            	ldir.mkdirs();
            }
            
            readImage(reader, lpng, 
              rectSize.left, rectSize.top, rectSize.width, 
              rectSize.height);
          }
        }
        catch (Exception e)
        {
          e.printStackTrace();
        }        
    }
    
  }
  
  private static void listFile(List<String> fileList,File file){  
      File[] fs = file.listFiles();  
      for(File f:fs){  
          if(f.isDirectory()) //若是目录，则递归该目录
          {	  
        	  listFile(fileList,f);  
          }
          else if(f.isFile())      //若是文件
          { 
              String filePath = f.getAbsolutePath();
              String Path = filePath.substring(0, filePath.lastIndexOf("."));
              String ext = filePath.substring(filePath.lastIndexOf(".") + 1);
              //System.err.println(filePath);
              //System.err.println(Path);
              //System.err.println(ext);
              if(ext.equals("plist")) {
            	  File pngfile = new File(Path + ".png");
            	  if(pngfile.exists())
            		  fileList.add(Path);
              }
          }
      }  
  }  
  
  public static void readImage(ImageReader reader, String dest, int l, int t, int w, int h)
    throws IOException
  {
    ImageReadParam param = reader.getDefaultReadParam();
    Rectangle rect = new Rectangle(l, t, w, h);
    param.setSourceRegion(rect);
    BufferedImage bi = reader.read(0, param);
    ImageIO.write(bi, "png", new File(dest));
  }
  
  public static List<RectSize> testXML(String plist)
    throws Exception
  {
    List<RectSize> list = new ArrayList();
    
    SAXReader reader = new SAXReader();
    Document doc = reader.read(plist);
    Element root = doc.getRootElement();
    Element dict = (Element)root.selectSingleNode("/plist/dict");
    for (Iterator<Element> iterator = dict.elementIterator(); iterator
          .hasNext();)
    {
      Element e = (Element)iterator.next();
      if ((e.getName().equalsIgnoreCase("key")) && 
        (e.getTextTrim().equalsIgnoreCase("frames")))
      {
        dict = (Element)iterator.next();
        break;
      }
    }
    if (dict == null) {
      throw new Exception("null");
    }
    for (Iterator<Element> it = dict.elementIterator(); it.hasNext();)
    {
      Element e = (Element)it.next();
      if ((e.getName().equalsIgnoreCase("key")) && 
        (it.hasNext()))
      {
        RectSize size = new RectSize(e.getTextTrim());
        e = (Element)it.next();
        if (e.getName().equalsIgnoreCase("dict"))
        {
          byte ff = 0;
          for (Iterator<Element> dit = e.elementIterator(); dit
                .hasNext();)
          {
            Element de = (Element)dit.next();
            if (ff == 0)
            {
              if ((de.getName().equalsIgnoreCase("key")) && 
                (de.getTextTrim().equalsIgnoreCase(
                "frame")))
              {
                de = (Element)dit.next();
                if (de.getName().equalsIgnoreCase("string"))
                {
                  String s = de.getTextTrim().replaceAll(
                    "[{}]", "");
                  String[] ss = s.split("[,]+");
                  if (ss.length == 4)
                  {
                    size.left = Integer.valueOf(ss[0])
                      .intValue();
                    size.top = Integer.valueOf(ss[1])
                      .intValue();
                    size.width = Integer.valueOf(ss[2])
                      .intValue();
                    size.height = 
                      Integer.valueOf(ss[3]).intValue();
                    ff = 1;
                  }
                }
              }
            }
            else
            {
              if (de.getName().equalsIgnoreCase("false"))
              {
                list.add(size);
                break;
              }
              if (de.getName().equalsIgnoreCase("true"))
              {
                size.width ^= size.height;
                size.height = (size.width ^ size.height);
                size.width ^= size.height;
                list.add(size);
                break;
              }
            }
          }
        }
      }
    }
    return list;
  }
}
