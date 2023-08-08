package org.example;

import com.crowdin.client.Client;
import com.crowdin.client.core.http.exceptions.HttpBadRequestException;
import com.crowdin.client.core.http.exceptions.HttpException;
import com.crowdin.client.core.model.Credentials;
import com.crowdin.client.core.model.ResponseObject;
import com.crowdin.client.sourcefiles.model.AddFileRequest;
import com.crowdin.client.sourcefiles.model.FileInfo;
import com.crowdin.client.storage.model.Storage;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Please specify the project ID, PAT, and wildcard pattern!");
            return;
        }

        String projectId = args[0];
        String token = args[1];
        String wildcard = args[2];

        if (!wildcard.contains("*")) {
            System.out.println("Invalid wildcard, please consider to have '*' in it.");
            return;
        }

        File currentDir = new File(".");
        File[] matchingFiles = currentDir.listFiles((dir, name) -> matchPatternWithRegex(name, wildcard));

        if (matchingFiles == null || matchingFiles.length == 0) {
            System.out.println("No files were found that match the wildcard.");
            return;
        }

        Credentials credentials = new Credentials(token, null);
        Client client = new Client(credentials);

        System.out.println("Start...");
        for (File file : matchingFiles) {
            String jsonString = buildJsonObject(file);

            try {
                callAddFileRequest(file, client, jsonString, projectId);
            } catch (HttpException e) {
                System.out.println("Invalid token: " + e.getError());
            } catch (HttpBadRequestException e) {
                System.out.println("File " + file.getName() + " already exists in Crowdin project.");
            }
        }
        System.out.println("Finish!");
    }

    private static void callAddFileRequest(File file, Client client, String jsonString, String projectId) {
        ResponseObject<Storage> storage = client.getStorageApi().addStorage(file.getName(), jsonString);

        AddFileRequest addFileRequest = new AddFileRequest();
        addFileRequest.setStorageId(storage.getData().getId());
        addFileRequest.setName(file.getName());
        addFileRequest.setType("auto");

        ResponseObject<? extends FileInfo> responseObject = client.getSourceFilesApi().addFile(Long.valueOf(projectId), addFileRequest);
        System.out.println("File " + responseObject.getData().getName() + " successfully uploaded!");
    }

    private static boolean matchPatternWithRegex(String name, String wildcard) {
        String regex;
        if (wildcard.contains("-")) {
            regex = "(\\d+)";
        } else {
            regex = "[^-]*";
        }
        Pattern pattern = Pattern.compile(wildcard.replace("*", regex));
        Matcher matcher = pattern.matcher(name);
        return matcher.matches();
    }

    private static String buildJsonObject(File file) {
        JsonObject jsonObject = new JsonObject();

        JsonObject dataObject = new JsonObject();
        dataObject.addProperty("id", 1);
        dataObject.addProperty("fileName", file.getName());

        jsonObject.add("data", dataObject);

        return new Gson().toJson(jsonObject);
    }
}