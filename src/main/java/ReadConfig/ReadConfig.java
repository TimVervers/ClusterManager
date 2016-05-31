/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ReadConfig;

import Interfaces.IRead;
import Models.ConfigModel;
import Models.SSHConnectionModel;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.io.File;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

/**
 *
 * @author ivan
 */
public class ReadConfig implements IRead{
    private static ArrayList<SSHConnectionModel> connectionData = null;
    private static ConfigModel confData = null;
    
    private static void readConnections() {
        try {	
            connectionData = new ArrayList<>();
            File inputFile = new File("./src/main/resources/ConnectionsConfig.xml");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();

            NodeList nList = doc.getElementsByTagName("server");

            for (int i = 0; i < nList.getLength(); i++) {
                Node nNode = nList.item(i);

                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;

                    connectionData.add(new SSHConnectionModel(eElement.getElementsByTagName("user").item(0).getTextContent(),
                                        eElement.getElementsByTagName("host").item(0).getTextContent(),
                                        Integer.parseInt(eElement.getElementsByTagName("port").item(0).getTextContent()),
                                        eElement.getElementsByTagName("passphrase").item(0).getTextContent(),
                                        eElement.getElementsByTagName("upload").item(0).getTextContent()));
                }
            }
      } catch (Exception e) {
         e.printStackTrace();
      }
    }
    
    private static void readConfigProperties() throws IOException {
        InputStream inputStream = null;
        try {
            confData = new ConfigModel();
            Properties prop = new Properties();
            String propFileNeme = "Config.properties";
            
            inputStream = ReadConfig.class.getClassLoader().getResourceAsStream(propFileNeme);
            
            if (inputStream != null) {
                prop.load(inputStream);
            } else {
                throw new FileNotFoundException("property file '" + propFileNeme + "' not fount in the classpath");
            }
            
            confData.setKeyPath(prop.getProperty("private-key-path"));
            confData.setKnownHostsPath(prop.getProperty("known-hosts-path"));
            confData.setDownloadFolderPath(prop.getProperty("download-folder"));
            
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        } finally {
            inputStream.close();
        }
    }
    
    public static ArrayList<SSHConnectionModel> getConnections() {
        if(connectionData == null)
            readConnections();
        
        return connectionData;
    }
    
    public static ConfigModel getConfigProperties() throws IOException {
        if(confData == null)
            readConfigProperties();
        
        return confData;
    }
}
