package com.emotionoui.oui.music.service;

import com.emotionoui.oui.common.exception.MusicException;
import com.emotionoui.oui.music.dto.req.SongReq;
import com.emotionoui.oui.music.dto.res.SongRes;
import com.emotionoui.oui.music.entity.MusicCollection;
import com.emotionoui.oui.music.repository.MusicMongoRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONArray;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import se.michaelthelin.spotify.SpotifyApi;

import javax.swing.text.Document;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class MusicServiceImpl implements MusicService{


    private final SpotifyApi spotifyApi;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Qualifier("englishFinderPattern")
    private final Pattern englishFinderPattern;

    @Qualifier("artistNameFilterPattern")
    private final Pattern artistNameFilterPattern;

    private final int RESPONSE_NUM = 5;
    private final String SPOTIFY_SEARCH_URL = "https://api.spotify.com/v1/search?locale=ko-KR,ko&type=track&market=KR&limit=5&offset=0&q=";
    private final String ITUNES_SEARCH_URL = "https://itunes.apple.com/search?limit=1&media=music&term=";

    private final MusicMongoRepository musicMongoRepository;

    @Override
    public void uploadSong() throws IOException {
//        String jsonFilePath = "C:\\music\\song_meta.json";
//        insertJsonData(jsonFilePath);

        String csvFilePath = "C:\\music\\song_tag.csv";
        updateMongoWithCsv(csvFilePath);
    }

    private void updateMongoWithCsv(String csvFilePath) throws IOException {
        List<String[]> data = readCsv(csvFilePath);

        for(int i=1; i< data.size(); ++i){
            String[] row = data.get(i);
            int id = Integer.parseInt(row[0]);

            String tags = row[1].replaceAll("[{}]", "").replaceAll("'", "")
                    .replaceAll("\"", "").replaceAll(" ", ""); // 필요없는 문자제거
            List<String> tagList = Arrays.asList(tags.split(","));

            MusicCollection document = musicMongoRepository.findByMusicId(id);
            document.setTags(tagList);
            musicMongoRepository.save(document);
        }
    }

    // CSV 파일 읽기 메서드
    private List<String[]> readCsv(String csvFilePath) throws IOException {
        List<String[]> data = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = splitCsvLine(line);
                data.add(values);
            }
        }
        return data;
    }

    // 괄호 안의 쉼표는 split 하지 않기 위한 함수
    private String[] splitCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean insideQuotes = false;

        for (char c : line.toCharArray()) {
            if (c == ',' && !insideQuotes) {
                values.add(sb.toString());
                sb = new StringBuilder();
            } else if (c == '"') {
                insideQuotes = !insideQuotes;
            } else {
                sb.append(c);
            }
        }

        // 마지막 값 추가
        values.add(sb.toString());

        return values.toArray(new String[0]);
    }

    private void insertJsonData(String jsonFilePath){
        ObjectMapper objectMapper = new ObjectMapper();
        // JSON 파일 읽기
        File jsonFile = new File(jsonFilePath);

        // JSON 파일을 Java 객체로 읽어오기
        List<MusicCollection> objects = null;
        try {
            objects = objectMapper.readValue(jsonFile, new TypeReference<List<MusicCollection>>() {});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        musicMongoRepository.saveAll(objects);
    }


//    private static void updateMongoWithCsv(MongoCollection<Document> collection, String csvFilePath) {
//        try {
//            List<String> lines = Files.readAllLines(Paths.get(csvFilePath));
//            for (String line : lines) {
//                String[] parts = line.split(",");
//                String songId = parts[0].trim();
//                String tags = parts[1].trim();
//
//                // MongoDB에서 해당 songId를 가진 문서 찾기
//                FindIterable<Document> findIterable = collection.find(new Document("song_id", songId));
//                MongoCursor<Document> cursor = findIterable.iterator();
//                while (cursor.hasNext()) {
//                    Document songDocument = cursor.next();
//                    List<String> songTags = songDocument.get("tags", ArrayList.class);
//                    if (songTags == null) {
//                        songTags = new ArrayList<>();
//                    }
//                    songTags.add(tags);
//                    songDocument.put("tags", songTags);
//
//                    // 문서 업데이트
//                    collection.replaceOne(new Document("song_id", songId), songDocument);
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }


    @Override
    public void uploadSongMeta(List<SongReq> songList) {
//        musicMongoRepository.saveAll(songList.stream()
//                .map(SongReq::toEntity)
//                .collect(Collectors.toList()));
    }

    @Override
    public String searchMusicURI(String artistName, String songName){
        ResponseEntity<String> responseEntity = requestMusicURIToSpotifyAPI(artistName, songName);

        int statusCode = responseEntity.getStatusCode().value();
        if(statusCode/100!=2){
            log.error("[MusicServiceImpl/searchMusicURI] SpotifyAPI 에러: {}", responseEntity.getBody());
            throw new MusicException(String.format("SpotifyAPI 에러 %s", responseEntity.getBody()));
        }

        for(int i=0; i<RESPONSE_NUM; ++i){
            try{
                JsonNode jsonNode = objectMapper.readTree(responseEntity.getBody());
                JsonNode trackRoot = jsonNode.get("tracks").get("items").get(i);
                Set<String> artistNamefound = trackRoot.get("artists")
                        .findValuesAsText("name")
                        .stream().collect(Collectors.toSet());
                String songNamefound = trackRoot.get("name").asText();
                String spotifyURI = trackRoot.get("uri").asText();

                if (!songNamefound.contains(songName) && !artistNamefound.stream().anyMatch((anf->artistName.contains(anf)))){
                    continue;
                }
                return spotifyURI;
            } catch(JsonProcessingException je){
                log.error("[MusicServiceImpl/searchMusicURI] json parsing 에러: {}", je.getMessage());
                throw new MusicException(String.format("json parsing 에러: %s", je.getMessage()));
            }
        }
        //throw new MusicException("노래명과 가수명에 해당하는 spotify uri가 존재하지 않습니다.");
        return null;
    }

    private String getEngSongName(String artistName, String songName){
        ResponseEntity<String> responseEntity = requestEngSongNameToiTunesAPI(artistName, songName);

        try{
            JsonNode jsonNode = objectMapper.readTree(responseEntity.getBody());
            JsonNode trackRoot = jsonNode.get("results").get(0);
            String songNamefound = trackRoot.get("trackName").asText();
            return songNamefound;
        } catch(JsonProcessingException je){
            log.error("[MusicServiceImpl/getEngSongName] json parsing 에러: {}", je.getMessage());
            throw new MusicException(String.format("json parsing 에러: %s", je.getMessage()));
        }
    }

    private ResponseEntity<String> requestEngSongNameToiTunesAPI(String artistName, String songName){
        HttpHeaders headers = new HttpHeaders();
        //headers.add("Content-type", "application/json");
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        StringBuilder urlBuilder = new StringBuilder();
        String artistNameEncoded = artistName.replace(" ", "+");
        String songNameEncoded = songName.replace(" ", "+");

        urlBuilder.append(ITUNES_SEARCH_URL)
                .append(artistNameEncoded)
                .append("+")
                .append(songNameEncoded);
        String url = urlBuilder.toString();

        log.info("[MusicServiceImpl/requestEngSongNameToiTunesAPI] request url: {}", url);

        try{
            ResponseEntity<String> responseEntity
                    = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
            //log.info("[MusicServiceImpl/requestEngSongNameToiTunesAPI] response: {}", responseEntity.getBody());
            return responseEntity;
        } catch(HttpClientErrorException e){
            log.error("[MusicServiceImpl/getEngSongName] iTunesAPI 에러: {}", e.getMessage());
            throw new MusicException("[MusicServiceImpl/getEngSongName] iTunesAPI 에러");
        }

    }

    private ResponseEntity<String> requestMusicURIToSpotifyAPI(String artistName, String songName){
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + spotifyApi.getAccessToken());
        headers.add("Host", "api.spotify.com");
        headers.add("Content-type", "application/json");
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        StringBuilder urlBuilder = new StringBuilder();


        String songNameEncoded = songName.substring(0);
        try{
            songNameEncoded = getEngSongName(artistName, songName);
        } catch(MusicException e){
            log.info("[MusicServieImpl/getEngSongName] {}", e.getMessage());
        } finally {
            songNameEncoded = songNameEncoded.replace(" ", "%20");
        }
        String songNameKorEncoded = songName.replace(" ", "%20");

        // TWS (투어스) -> TWS, 임창정 -> 임창정, 비비 (BIBI) -> BIBI
        String artistNameEncoded = artistName.substring(0);
        if(englishFinderPattern.matcher(artistNameEncoded).find()){
            artistNameEncoded = artistNameFilterPattern.matcher(artistNameEncoded).replaceAll("").trim();
        }
        artistNameEncoded = artistNameEncoded.replace(" ", "%20");
        urlBuilder.append(SPOTIFY_SEARCH_URL)
                .append(songNameEncoded)
                .append("%20track:")
                .append(songNameKorEncoded)
                .append("%20artist:")
                .append(artistNameEncoded);
        String url = urlBuilder.toString();
        log.info("[MusicServiceImpl/requestMusicURIToSpotifyAPI] request url: {}", url);

        ResponseEntity<String> responseEntity
                = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
        //log.info("[MusicServiceImpl/requestMusicURIToSpotifyAPI] response: {}", responseEntity.getBody());
        return responseEntity;
    }
}
