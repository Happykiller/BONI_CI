package com.bonitasoft.app;


import org.bonitasoft.engine.api.ApiAccessType;
import org.bonitasoft.engine.api.LoginAPI;
import org.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.engine.util.APITypeManager;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;

import java.io.BufferedReader;
import java.io.File;
import java.util.*;

/**
 * Created by HAPPYBONITA on 28/09/2015.
 */
public class Main {

    private static Properties prop;

    private static List<Map<String, String>> arguments;

    public static void main(String [ ] args){
        // init message system
        Library.initMessage();

        try {
            // load properties
            prop = Library.load("BONI_CI.properties");
            if(prop == null){
                throw new Exception("No properties found.");
            }

            // start message
            Library.message("===== "+prop.getProperty("app.name")+" - START ======", false, true);

            // loading args
            arguments = Library.getArgs(args);
            if(arguments == null){
                throw new Exception("No arguments detected, "+prop.getProperty("app.name")+" stop.");
            }

            // swich mopde
            String s = Library.getArgumentValue(arguments, "mode");
            if (s.equals("test")) {
                testConnection();
            } if (s.equals("updateRepoSvn")) {
                updateRepoSvn();
            } if (s.equals("generateBonitaArtifact")) {
                generateBonitaArtifact();
            } else {
                throw new IllegalArgumentException("Invalid argument of mode");
            }

            // end message
            Library.message("===== "+prop.getProperty("app.name")+" - END ======", false, true);
        }catch (Exception e){
            Library.traceExeption(e);
        }
    }

    /**
     *
     */
    public static void testConnection(){
        try{
            // start message
            Library.message("+ testConnection : start", false, true);

            // Setup access type (HTTP on local host)
            Map<String, String> parameters = new HashMap<String, String>();
            parameters.put("server.url", prop.getProperty("bonitaBPM.serverUrl"));
            parameters.put("application.name", prop.getProperty("bonitaBPM.applicationName"));
            APITypeManager.setAPITypeAndParams(ApiAccessType.HTTP, parameters);

            // Authenticate and obtain API session
            LoginAPI loginAPI = TenantAPIAccessor.getLoginAPI();
            APISession session = loginAPI.login(prop.getProperty("bonitaBPM.techUserLog"), prop.getProperty("bonitaBPM.techUserPass"));

            // Operation
            Long id = TenantAPIAccessor.getIdentityAPI(session).getUserByUserName("walter.bates").getId();
            Library.message("Walter.bates id:"+id.toString(), false, true);

            // logout
            loginAPI.logout(session);

            // end message
            Library.message("+ testConnection : end", false, true);
        }catch (Exception e){
            Library.traceExeption(e);
        }
    }

    /**
     *
     */
    public static void generateBonitaArtifact(){
        try{
            // start message
            Library.message("+ generateBonitaArtifact : start", false, true);

            String cmd = prop.getProperty("bonitaStudio.home.path") + File.separator + "workspace_api_scripts" + File.separator;

            if (Library.isWindows()) {
                cmd += "BonitaStudioBuilder.bat";
            } else if (Library.isMac()) {
                cmd += "BonitaStudioBuilder_Mac.sh";
            } else if (Library.isUnix()) {
                cmd += "BonitaStudioBuilder.sh";
            } else {
                System.out.println("Your OS is not support!!");
            }

            cmd += " -repoPath="+prop.getProperty("bonitaStudio.home.path") + File.separator + prop.getProperty("bonitaStudio.repo.path")+" -outputFolder="+prop.getProperty("build.path")+" -buildAll -environment=locale -migrate";

            Library.message(cmd, false, false);

            Process p = Runtime.getRuntime().exec(cmd);

            BufferedReader output =Library.getOutput(p);
            BufferedReader error = Library.getError(p);
            String ligne = "";

            while ((ligne = output.readLine()) != null) {
                Library.message(ligne, false, true);
            }

            while ((ligne = error.readLine()) != null) {
                Library.message(ligne, false, true);
            }

            p.waitFor();

            // end message
            Library.message("+ generateBonitaArtifact : end", false, true);
        }catch (Exception e){
            Library.traceExeption(e);
        }
    }

    /**
     *
     */
    public static void updateRepoSvn(){
        try{
            // start message
            Library.message("+ updateRepoSvn : start", false, true);

            SVNClientManager ourClientManager = SVNClientManager.newInstance();
            SVNUpdateClient updateClient = ourClientManager.getUpdateClient();

            String path = prop.getProperty("bonitaStudio.home.path") + File.separator + prop.getProperty("bonitaStudio.repo.path");
            Long revision = updateClient.doUpdate(new File(path) , SVNRevision.HEAD, SVNDepth.INFINITY, true, true);

            Library.message("- revision : " + revision.toString(), false, true);

            // end message
            Library.message("+ updateRepoSvn : end", false, true);
        }catch (Exception e){
            Library.traceExeption(e);
        }
    }
}
