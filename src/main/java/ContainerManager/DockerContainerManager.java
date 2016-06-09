package ContainerManager;

import Models.ServerModel;
import Models.ContainerModel;
import Connection.ExecuteCommand;
import Connection.SFTPConnection;
import Connection.SSHConnection;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.schmizz.sshj.SSHClient;
import Interfaces.ContainerManager;
import nl.hogeschool.ClusterManager.ListHelper;
import nl.hogeschool.ClusterManager.Tools;
import org.json.simple.JSONObject;

public class DockerContainerManager implements ContainerManager {
    private JSONObject apiData;
    private List<ServerModel> listOfServersWithContainers = null;
    private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    @Override
    public void setJson(JSONObject data) {
        apiData = data;
    }
    
    @Override
    public int startContainer() throws IOException {
        try {
            String container_id = apiData.get("cId").toString();
            String server_ip = getServerAndContainerInfoByContainerID(container_id).get("ip");
            ExecuteCommand.execute(server_ip, "docker start " + container_id);
            return 1;
        } catch (InterruptedException ex) {
            Logger.getLogger(DockerContainerManager.class.getName()).log(Level.SEVERE, null, ex);
            return 0;
        }
    }

    @Override
    public int stopContainer() throws IOException {
        try {
            String container_id = apiData.get("cId").toString(); 
            String server_ip = getServerAndContainerInfoByContainerID(container_id).get("ip");
            ExecuteCommand.execute(server_ip, "docker stop " + container_id);
            return 1;
        } catch (InterruptedException ex) {
            Logger.getLogger(DockerContainerManager.class.getName()).log(Level.SEVERE, null, ex);
            return 0;
        }
    }
    
    @Override
    public int removeContainer() throws IOException {
        try {
            String container_id = apiData.get("cId").toString();
            String server_ip = getServerAndContainerInfoByContainerID(container_id).get("ip");
            ExecuteCommand.execute(server_ip, "docker rm " + container_id);
            return 1;
        } catch (InterruptedException ex) {
            Logger.getLogger(DockerContainerManager.class.getName()).log(Level.SEVERE, null, ex);
            return 0;
        }
    }

    @Override
    public int moveContainer() throws IOException {
        try {
            String container_id = apiData.get("cId").toString();
            String home_ip = getServerAndContainerInfoByContainerID(container_id).get("ip");
            String destination_ip = apiData.get("destination").toString();
            String oldContainerLocation;
            String newContainerLocation;
                    
            if(Tools.searchUploadPath(home_ip) == null){
                LOGGER.warning("Can not find the server");
                return 0;
            } else {
                oldContainerLocation = Tools.searchUploadPath(home_ip).getUploadPath().concat(container_id +".tar");
            }
            
            if(Tools.searchUploadPath(destination_ip) == null){
                LOGGER.warning("Can not find the server");
                return 0;
            } else {
                newContainerLocation = Tools.searchUploadPath(destination_ip).getUploadPath().concat(container_id +".tar");
            }

            SFTPConnection sftpTransfer = new SFTPConnection();

            // Export container to tar file in the ...
            ExecuteCommand.execute(home_ip, "docker export --output=\"" + oldContainerLocation + "\" " + container_id);
            sftpTransfer.downloadFile(home_ip, container_id);
            sftpTransfer.uploadFile(destination_ip, container_id);
            ExecuteCommand.execute(destination_ip, "cat " + newContainerLocation + " | docker import - " + container_id + ":latest");
            ExecuteCommand.execute(destination_ip, "docker run "+container_id + ":latest "+getServerAndContainerInfoByContainerID(container_id).get("command"));
            
            return 1;
        } catch (Exception e) {
            LOGGER.warning(e.getMessage());
            return 0;
        }
    }
    
    @Override
    public int createContainer() throws IOException {
        String destination_ip = apiData.get("destination").toString();
        String image = apiData.get("image").toString();
        try { 
            ExecuteCommand.execute(destination_ip, "docker run " + image);
            return 1;
        } catch (InterruptedException ex) {
            Logger.getLogger(DockerContainerManager.class.getName()).log(Level.SEVERE, null, ex);
            return 0;
        }
    }
    
    @Override
    public int renameContainer() throws IOException{
        String container_id = apiData.get("cId").toString();
        String server_ip = getServerAndContainerInfoByContainerID(container_id).get("ip");
        String newName = apiData.get("newName").toString();
        try { 
            ExecuteCommand.execute(server_ip, "docker rename "+container_id+" "+newName);
            return 1;
        } catch (InterruptedException ex) {
            Logger.getLogger(DockerContainerManager.class.getName()).log(Level.SEVERE, null, ex);
            return 0;
        }
    }

    @Override
    public void getAllContainers() throws IOException, InterruptedException {
        HashMap<String, SSHClient> listOfClients = SSHConnection.getListOfClients();

        for (Entry<String, SSHClient> client : listOfClients.entrySet()) {
            String tempServerIP = client.getKey();
            InputStream resultOfExecute = ExecuteCommand.execute(tempServerIP, "docker ps -a");
            ListHelper.addOutputToList(resultOfExecute, tempServerIP,"Docker");
        }
    }

    @Override
    public HashMap<String, String> getServerAndContainerInfoByContainerID(String containerID) {
        try {
            listOfServersWithContainers = ListHelper.getListOfServersAndContainers();
        } catch (Exception e) {
            LOGGER.warning(e.getMessage());
        }
      
        HashMap<String, String> containerElements = new HashMap();
        for (ServerModel server : listOfServersWithContainers) {
            for (ContainerModel theContainer : server.getContainers()) {
                if (theContainer.getContainerID().contains(containerID)) {
                    String server_ip = server.getIPAddress();
                    String command = theContainer.getContainerCommand();
                    containerElements.put("ip", server_ip);
                    containerElements.put("command", command);
                }
            }
        }
        return containerElements;
    }
}