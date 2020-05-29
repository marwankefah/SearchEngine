package com.apt;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.annotation.MultipartConfig;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

//import edu.cmu.sphinx.api.Configuration;
//import edu.cmu.sphinx.api.SpeechResult;
//import edu.cmu.sphinx.api.StreamSpeechRecognizer;
@MultipartConfig
public class VoiceHandler extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "*");
        resp.setHeader("Access-Control-Allow-Headers", "*");


//        Configuration configuration = new Configuration();
//
//        configuration.setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us");
//        configuration.setDictionaryPath("resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict");
//        configuration.setLanguageModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us.lm.bin");
//
//        StreamSpeechRecognizer recognizer = new StreamSpeechRecognizer(configuration);
//        InputStream stream = req.getPart("voice").getInputStream();
//
//        recognizer.startRecognition(stream);
//        SpeechResult result;
//        String APIResult = "";
//        while ((result = recognizer.getResult()) != null) {
//            APIResult += result.getHypothesis();
//        }
//        recognizer.stopRecognition();

        resp.getWriter().println("");
    }
}
