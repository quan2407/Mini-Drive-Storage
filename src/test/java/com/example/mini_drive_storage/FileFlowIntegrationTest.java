package com.example.mini_drive_storage;

import com.jayway.jsonpath.JsonPath;
import jakarta.transaction.Transactional;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FileFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private String tokenUserA;
    private String tokenUserB;
    private UUID fileId;

    @Test
    void fullFlow_register_login_upload_share_download_success() throws Exception {

        register("a@test.com", "123456");
        register("b@test.com", "123456");

        tokenUserA = loginAndGetToken("a@test.com", "123456");

        fileId = uploadFile(tokenUserA);

        shareFile(fileId, tokenUserA, "b@test.com", "VIEW");

        tokenUserB = loginAndGetToken("b@test.com", "123456");

        downloadFile(tokenUserB, fileId);
    }

    // ================= helpers =================

    private void register(String email, String password) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email": "%s",
                              "password": "%s"
                            }
                            """.formatted(email, password)))
                .andExpect(status().isOk());
    }

    private String loginAndGetToken(String email, String password) throws Exception {

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "email": "%s",
                          "password": "%s"
                        }
                        """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();

        // vì API trả về text/plain
        return result.getResponse().getContentAsString();
    }


    private UUID uploadFile(String token) throws Exception {

        MockMultipartFile file = new MockMultipartFile(
                "files",
                "hello.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "hello integration test".getBytes()
        );

        MvcResult result = mockMvc.perform(
                        multipart("/api/v1/files")
                                .file(file)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists())
                .andReturn();

        return UUID.fromString(
                JsonPath.read(
                        result.getResponse().getContentAsString(),
                        "$[0].id"
                )
        );
    }


    private void shareFile(UUID fileId, String token, String email, String permission)
            throws Exception {

        mockMvc.perform(post("/api/v1/files/{id}/share", fileId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "email": "%s",
                          "permission": "%s"
                        }
                        """.formatted(email, permission))
                )
                .andExpect(status().isOk());
    }


    private void downloadFile(String token, UUID fileId) throws Exception {

        mockMvc.perform(get("/api/v1/{id}/download", fileId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        Matchers.containsString("hello.txt")
                ));
    }
}
