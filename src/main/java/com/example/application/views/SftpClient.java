package com.example.application.views;

import com.example.application.data.entity.FTPFile;
import com.example.application.utils.TaskStatus;
import com.example.application.utils.Util;
import com.jcraft.jsch.*;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;

import java.io.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

/**
 * A simple SFTP client using JSCH http://www.jcraft.com/jsch/
 */
public final class SftpClient {
    private final String      host;
    private final int         port;
    private final String      username;
    private final JSch        jsch;
    private       ChannelSftp channel;
    private       Session     session;

    /**
     * @param host     remote host
     * @param port     remote port
     * @param username remote username
     */
    public SftpClient(String host, int port, String username) {
        this.host     = host;
        this.port     = port;
        this.username = username;
        jsch          = new JSch();
    }

    /**
     * Use default port 22
     *
     * @param host     remote host
     * @param username username on host
     */
    public SftpClient(String host, String username) {
        this(host, 22, username);
    }

    /**
     * Authenticate with remote using password
     *
     * @param password password of remote
     * @throws JSchException If there is problem with credentials or connection
     */
    public void authPassword(String password) throws JSchException {
        session = jsch.getSession(username, host, port);
        //disable known hosts checking
        //if you want to set knows hosts file You can set with jsch.setKnownHosts("path to known hosts file");
        var config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.setPassword(password);
        session.connect();
        channel = (ChannelSftp) session.openChannel("sftp");
        channel.connect();
    }


    public void authKey(String key, String pass) throws JSchException {
        byte[] privateKey = key.getBytes();
        jsch.addIdentity("identity_name", privateKey, null, pass != null ? pass.getBytes() : null);
      //  jsch.addIdentity(key, pass);
        //jsch.addIdentity(keyPath, pass);
        session = jsch.getSession(username, host, port);
        //disable known hosts checking
        //if you want to set knows hosts file You can set with jsch.setKnownHosts("path to known hosts file");
        var config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.connect();
     //   session.setTimeout(6000);
        channel = (ChannelSftp) session.openChannel("sftp");
        channel.connect();
    }

    /**
     * List all files including directories
     *
     * @param remoteDir Directory on remote from which files will be listed
     * @throws SftpException If there is any problem with listing files related to permissions etc
     * @throws JSchException If there is any problem with connection
     */
    @SuppressWarnings("unchecked")
    public void listFiles(String remoteDir) throws SftpException, JSchException {
        if (channel == null) {
            throw new IllegalArgumentException("Connection is not available");
        }
        System.out.printf("Listing [%s]...%n", remoteDir);
        channel.cd(remoteDir);
        Vector<ChannelSftp.LsEntry> files = channel.ls(".");
        for (ChannelSftp.LsEntry file : files) {
            var name        = file.getFilename();
            var attrs       = file.getAttrs();
            var permissions = attrs.getPermissionsString();
            var size        = Util.humanReadableByteCount(attrs.getSize());
            if (attrs.isDir()) {
                size = "PRE";
            }
            System.out.printf("[%s] %s(%s) Atime: %s %n", permissions, name, size, localTimeUtc(Instant.ofEpochSecond(attrs.getMTime())));
        }
    }


    public List<FTPFile> getFiles(String remoteDir, Long fromDate, Long toDate) throws SftpException, JSchException {
        if (channel == null) {
            throw new IllegalArgumentException("Connection is not available");
        }
        System.out.printf("Listing [%s]...%n", remoteDir);
        channel.cd(remoteDir);

        List<FTPFile>_files=new ArrayList<FTPFile>();

        Vector<ChannelSftp.LsEntry> files = channel.ls(".");
        for (ChannelSftp.LsEntry file : files) {
            FTPFile f = new FTPFile();


            var name        = file.getFilename();
            var attrs       = file.getAttrs();
            var permissions = attrs.getPermissionsString();
            var size        = Util.humanReadableByteCount(attrs.getSize());

            f.setSize(attrs.getSize());
            f.setName(file.getFilename());
            f.setErstellungszeit(localTimeUtc(Instant.ofEpochSecond(attrs.getMTime())));

            if (attrs.isDir()) {
                size = "<DIR>";
            }
        //    System.out.printf("[%s] %s(%s) Atime: %s %n", permissions, name, size, localTimeUtc(Instant.ofEpochSecond(attrs.getMTime())));

            if (attrs.getMTime() >= fromDate && attrs.getMTime()<= toDate) {
                _files.add(f);
            }
        }

        return _files;

    }

    public static LocalDateTime localTimeUtc(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.systemDefault());
    }

    /**
     * Upload a file to remote
     *
     * @param localPath  full path of location file
     * @param remotePath full path of remote file
     * @throws JSchException If there is any problem with connection
     * @throws SftpException If there is any problem with uploading file permissions etc
     */
    public void uploadFile(String localPath, String remotePath) throws JSchException, SftpException {
        System.out.printf("Uploading [%s] to [%s]...%n", localPath, remotePath);
        if (channel == null) {
            throw new IllegalArgumentException("Connection is not available");
        }
        channel.put(localPath, remotePath);
    }

    /**
     * Download a file from remote
     *
     * @param remotePath full path of remote file
     * @param localPath  full path of where to save file locally
     * @throws SftpException If there is any problem with downloading file related permissions etc
     */
    public void downloadFile(String remotePath, String localPath) throws SftpException {
        System.out.printf("Downloading [%s] to [%s]...%n", remotePath, localPath);
        if (channel == null) {
            throw new IllegalArgumentException("Connection is not available");
        }
        channel.get(remotePath, localPath);
    }

    /**
     * Delete a file on remote
     *
     * @param remoteFile full path of remote file
     * @throws SftpException If there is any problem with deleting file related to permissions etc
     */
    public void delete(String remoteFile) throws SftpException {
        System.out.printf("Deleting [%s]...%n", remoteFile);
        if (channel == null) {
            throw new IllegalArgumentException("Connection is not available");
        }
        channel.rm(remoteFile);
    }


    public byte[] readFile(String fileName) throws SftpException, IOException {

        System.out.printf("get Bytes from File [%s]%n", fileName);
        if (channel == null) {
            throw new IllegalArgumentException("Connection is not available");
        }
        InputStream inputStream = channel.get(fileName);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            var fileData = outputStream.toByteArray();

            outputStream.close();
            inputStream.close();
            channel.disconnect();
            session.disconnect();

        return fileData;
    }


    public void TailRemoteLogFile(TextArea logTextArea, String FileName, TaskStatus stat  ) throws JSchException, IOException, SftpException {

        VaadinSession vaadinSession = VaadinSession.getCurrent();
        VaadinService vaadinService = VaadinService.getCurrent();
        UI ui = UI.getCurrent();
        clearLog(logTextArea);
        if (channel == null) {
            throw new IllegalArgumentException("Connection is not available");
        }

        ChannelSftp sftpChannel = (ChannelSftp) channel;


        String command = "tail -f " +FileName;
      //  String command = "tail -300 " +FileName;
        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand(command);


    new Thread(() -> {
        Thread.currentThread().setName("Tail-Thread");
        VaadinService.setCurrent(vaadinService);
        VaadinSession.setCurrent(vaadinSession);
        UI.setCurrent(ui);

    try {
        channel.connect();
    } catch (JSchException e) {
        throw new RuntimeException(e);
    }

    // Read the output of the tail command and display it
    BufferedReader reader = null;
    try {
        reader = new BufferedReader(new InputStreamReader(channel.getInputStream()));
    } catch (IOException e) {
        throw new RuntimeException(e);
    }
        String line="";
        while (!Thread.currentThread().isInterrupted()) {
            try {

                line = reader.readLine();
              //  if (line.startsWith("stop")) break;
                if (!stat.isActive()) break;
                if (line==null) break;
            } catch (IOException e) {
                //throw new RuntimeException(e);
                System.out.println("readLine-Abfrage in SftpClient unterbrochen " + e.getMessage() );
                sftpChannel.exit();
                session.disconnect();
            }
            //  System.out.println(line);
            updateLog(line,logTextArea);
        }

        sftpChannel.exit();
        session.disconnect();

    }).start();

    }

  /*  public void ReadRemoteLogFile(UI ui, TextArea logTextArea, String FileName, TaskStatus stat  ) throws JSchException, IOException, SftpException {

        if (channel == null) {
            throw new IllegalArgumentException("Connection is not available");
        }

       // ChannelSftp sftpChannel = (ChannelSftp) channel;


        //String command = "tail -f " +FileName;
        String command = "tail -300 " +FileName;
        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand(command);



            try {
                channel.connect();
            } catch (JSchException e) {
                throw new RuntimeException(e);
            }

            // Read the output of the tail command and display it
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(channel.getInputStream()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            String line;
            while (true) {
                try {
                    line = reader.readLine();
                    if (line==null) break;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                //  System.out.println(line);
                updateLog(line,logTextArea);
            }

         //   sftpChannel.exit();
            session.disconnect();

//        logTextArea.getElement().executeJs("this.scrollTop = this.scrollHeight");


    }*/


    public void ReadRemoteLogFile(UI ui, TextArea logTextArea, String FileName) throws JSchException, IOException, SftpException {

        if (channel == null) {
            throw new IllegalArgumentException("Connection is not available");
        }

        // ChannelSftp sftpChannel = (ChannelSftp) channel;


        //String command = "tail -f " +FileName;
        String command = "head -300 " +FileName;
        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand(command);



        try {
            channel.connect();
        } catch (JSchException e) {
            throw new RuntimeException(e);
        }

        // Read the output of the tail command and display it
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(channel.getInputStream()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String line;

        clearLog(logTextArea);

        while (true) {
            try {
                line = reader.readLine();
                if (line==null) break;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            //  System.out.println(line);
            updateLog(line,logTextArea);
        }

        //####################

       // command = "head 300 " +FileName;
        command = "tail -300 " +FileName;

        channel.disconnect();
        channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand(command);



        try {
            channel.connect();
        } catch (JSchException e) {
            throw new RuntimeException(e);
        }

        // Read the output of the tail command and display it
        reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(channel.getInputStream()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        updateLog("###################### TAIL -300 ##############################",logTextArea);
        while (true) {
            try {
                line = reader.readLine();
                if (line==null) break;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            //  System.out.println(line);
            updateLog(line,logTextArea);
        }



        //   sftpChannel.exit();
        session.disconnect();



//        logTextArea.getElement().executeJs("this.scrollTop = this.scrollHeight");


    }


    private void updateLog(String line, TextArea tailTextArea) {
        VaadinSession.getCurrent().lock();

        tailTextArea.getElement().executeJs(
                "this.inputElement.value += $0; this._updateHeight(); this._inputField.scrollTop = this._inputField.scrollHeight - this._inputField.clientHeight;",
                "\n" + line
        );
        VaadinSession.getCurrent().unlock();
    }

    private void clearLog(TextArea tailTextArea) {
        VaadinSession.getCurrent().lock();

        tailTextArea.getElement().executeJs(
                "this.inputElement.value = $0; this._updateHeight(); this._inputField.scrollTop = this._inputField.scrollHeight - this._inputField.clientHeight;",
                ""
        );
        VaadinSession.getCurrent().unlock();
    }




    /**
     * Disconnect from remote
     */
    public void close() {
        if (channel != null) {
            channel.exit();
        }
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
    }
}
