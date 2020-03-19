package cn.kong.web.controller;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.annotation.PostConstruct;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

@RestController
@RequestMapping("/dynamic-extraction-config")
public class DynamicExtractConfigController {

    @Value("${extraction-config.root-path}")
    private String fileRootPath;
    private String generatedFilePath;
    private String tableConfigPath;

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm");

    @PostConstruct
    public void postInit() {
        generatedFilePath = Paths.get(fileRootPath, "generated").toString();
        tableConfigPath = Paths.get(fileRootPath, "table-config").toString();
    }

    @GetMapping
    public ResponseEntity<FileSystemResource> getConfig() throws ParserConfigurationException, IOException, TransformerException {

        String downloadFilename = "BIExtraction.xml";
        String cachedFilename = String.format("BIExtraction.%s.xml", sdf.format(new Date()));

        FileSystemResource fsr;

        File cachedFile = new File(Paths.get(generatedFilePath, cachedFilename).toString());

        if (!cachedFile.exists()) {
            File tableConfigDir = new File(tableConfigPath);
            assert tableConfigDir.isDirectory();
            ExtractionXmlConfigBuilder xmlConfigBuilder = new ExtractionXmlConfigBuilder();
            FilenameFilter filenameFilter = new SuffixFileFilter(".json", IOCase.INSENSITIVE);
            for (File f : Objects.requireNonNull(tableConfigDir.listFiles(filenameFilter))) {
                JSONObject jsonObject = JSONObject.parseObject(new FileInputStream(f.getPath()), StandardCharsets.UTF_8, Object.class);
                xmlConfigBuilder.appendExtractionNodeFromJsonObject(jsonObject);
            }

            ExtractionXmlConfigWriter.write(xmlConfigBuilder.getDocument(), cachedFile.toString());
        }

        fsr = new FileSystemResource(cachedFile);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
        headers.add("Content-Disposition", "attachment; filename=" + downloadFilename);
        headers.add("Pragma", "no-cache");
        headers.add("Expires", "0");
        headers.add("Last-Modified", new Date().toString());
        headers.add("ETag", String.valueOf(System.currentTimeMillis()));
        return ResponseEntity.ok().headers(headers).contentLength(cachedFile.length()).contentType(MediaType.parseMediaType("application/octet-stream")).body(fsr);
    }
}
