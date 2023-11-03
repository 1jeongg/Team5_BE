package com.kakao.sunsuwedding.review;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakao.sunsuwedding._core.security.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@Import({
        SecurityConfig.class,
})
@ActiveProfiles("test")
@Sql("classpath:db/teardown.sql")
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public class ReviewRestControllerTest {
    private static final Logger logger = LoggerFactory.getLogger(ReviewRestControllerTest.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper om;


    // ============ 리뷰 등록 테스트 ============
    @DisplayName("리뷰 등록 성공 테스트")
    @Test
    @WithUserDetails("couple@gmail.com")
    public void add_review_success_test() throws Exception {
        // given
        Long chatId = 1L;
        ReviewRequest.AddDTO request = new ReviewRequest.AddDTO("최고의 플래너!");

        String requestBody = om.writeValueAsString(request);

        // when
        ResultActions result = mockMvc.perform(
                MockMvcRequestBuilders
                        .post("/reviews")
                        .param("chatId", String.valueOf(chatId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        );

        logResult(result);

        // then
        result.andExpect(MockMvcResultMatchers.jsonPath("$.success").value("true"));
    }

    @DisplayName("리뷰 등록 실패 테스트 1 - 존재하지 않는 채팅방")
    @Test
    @WithUserDetails("couple@gmail.com")
    public void add_review_fail_test_match_not_found() throws Exception {
        // given
        Long chatId = 80L;
        ReviewRequest.AddDTO request = new ReviewRequest.AddDTO("최고의 플래너!");

        String requestBody = om.writeValueAsString(request);

        // when
        ResultActions result = mockMvc.perform(
                MockMvcRequestBuilders
                        .post("/reviews")
                        .param("chatId", String.valueOf(chatId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        );

        logResult(result);

        // then
        result.andExpect(MockMvcResultMatchers.jsonPath("$.success").value("false"));
        result.andExpect(MockMvcResultMatchers.jsonPath("$.error.status").value(404));
        result.andExpect(MockMvcResultMatchers.jsonPath("$.error.message").value("매칭 내역을 찾을 수 없습니다."));
    }

    @DisplayName("리뷰 등록 실패 테스트 2 - 다른 유저의 매칭")
    @Test
    @WithUserDetails("couple@gmail.com")
    public void add_review_fail_test_not_my_match() throws Exception {
        // given
        Long chatId = 3L;
        ReviewRequest.AddDTO request = new ReviewRequest.AddDTO("최고의 플래너!");

        String requestBody = om.writeValueAsString(request);

        // when
        ResultActions result = mockMvc.perform(
                MockMvcRequestBuilders
                        .post("/reviews")
                        .param("chatId", String.valueOf(chatId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        );

        logResult(result);

        // then
        result.andExpect(MockMvcResultMatchers.jsonPath("$.success").value("false"));
        result.andExpect(MockMvcResultMatchers.jsonPath("$.error.status").value(403));
        result.andExpect(MockMvcResultMatchers.jsonPath("$.error.message").value("사용할 수 없는 기능입니다."));
    }

    @DisplayName("리뷰 등록 실패 테스트 3 - 전체확정 되지 않음")
    @Test
    @WithUserDetails("couple@gmail.com")
    public void add_review_fail_test_match_not_confirmed() throws Exception {
        // given
        Long chatId = 2L;
        ReviewRequest.AddDTO request = new ReviewRequest.AddDTO("최고의 플래너!");

        String requestBody = om.writeValueAsString(request);

        // when
        ResultActions result = mockMvc.perform(
                MockMvcRequestBuilders
                        .post("/reviews")
                        .param("chatId", String.valueOf(chatId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        );

        logResult(result);

        // then
        result.andExpect(MockMvcResultMatchers.jsonPath("$.success").value("false"));
        result.andExpect(MockMvcResultMatchers.jsonPath("$.error.status").value(400));
        result.andExpect(MockMvcResultMatchers.jsonPath("$.error.message").value("견적서 전체 확정이 되지 않았습니다."));
    }



    // ============ 리뷰 조회 테스트 ============
    // 1. planner로 조회
    @DisplayName("플래너별 리뷰 조회 성공 테스트")
    @Test
    @WithUserDetails("couple@gmail.com")
    public void find_review_by_planner_success_test() throws Exception {
        // given
        int page = 0;
        ReviewRequest.FindAllByPlannerDTO request = new ReviewRequest.FindAllByPlannerDTO(2L);

        String requestBody = om.writeValueAsString(request);

        // when
        ResultActions result = mockMvc.perform(
                MockMvcRequestBuilders
                        .get("/reviews")
                        .param("page", String.valueOf(page))
                        .content(requestBody)
                        .contentType(MediaType.APPLICATION_JSON)
        );

        logResult(result);

        // then
        result.andExpect(MockMvcResultMatchers.jsonPath("$.success").value("true"));
        result.andExpect(MockMvcResultMatchers.jsonPath("$.response.reviews[0].id").value(1));
    }


    // 2. couple로 조회
    @DisplayName("커플별로 리뷰 조회 성공 테스트")
    @Test
    @WithUserDetails("couple@gmail.com")
    public void find_review_by_couple_success_test() throws Exception {
        // when
        ResultActions result = mockMvc.perform(
                MockMvcRequestBuilders
                        .get("/reviews/collect")
        );

        logResult(result);

        // then
        result.andExpect(MockMvcResultMatchers.jsonPath("$.success").value("true"));
        result.andExpect(MockMvcResultMatchers.jsonPath("$.response.reviews[0].id").value(1));
    }

    @DisplayName("커플별로 리뷰 조회 실패 테스트 1 - 플래너가 요청")
    @Test
    @WithUserDetails("planner@gmail.com")
    public void find_review_by_couple_fail_test_planner_request() throws Exception {
        // when
        ResultActions result = mockMvc.perform(
                MockMvcRequestBuilders
                        .get("/reviews/collect")
        );

        logResult(result);

        // then
        result.andExpect(MockMvcResultMatchers.jsonPath("$.success").value("false"));
        result.andExpect(MockMvcResultMatchers.jsonPath("$.error.message").value("사용할 수 없는 기능입니다."));
    }

    // 3. reviewId로 조회
    @DisplayName("리뷰 id로 조회 성공 테스트")
    @Test
    @WithUserDetails("couple@gmail.com")
    public void find_review_by_id_success_test() throws Exception {
        // given
        Long reviewId = 1L;
        // when
        ResultActions result = mockMvc.perform(
                MockMvcRequestBuilders
                        .get("/reviews/{reviewId}", reviewId)
        );

        logResult(result);

        // then
        result.andExpect(MockMvcResultMatchers.jsonPath("$.success").value("true"));
        result.andExpect(MockMvcResultMatchers.jsonPath("$.response.id").value(1));
        result.andExpect(MockMvcResultMatchers.jsonPath("$.response.plannerName").value("planner"));
        result.andExpect(MockMvcResultMatchers.jsonPath("$.response.content").value("최고의 플래너!"));
    }

    @DisplayName("리뷰 id로 조회 실패 테스트 1 - 플래너가 요청")
    @Test
    @WithUserDetails("planner@gmail.com")
    public void find_review_by_id_fail_test_planner_request() throws Exception {
        // given
        Long reviewId = 1L;
        // when
        ResultActions result = mockMvc.perform(
                MockMvcRequestBuilders
                        .get("/reviews/{reviewId}", reviewId)
        );

        logResult(result);

        // then
        result.andExpect(MockMvcResultMatchers.jsonPath("$.success").value("false"));
        result.andExpect(MockMvcResultMatchers.jsonPath("$.error.message").value("사용할 수 없는 기능입니다."));
    }



    // ============ 리뷰 수정 테스트 ============
    @DisplayName("리뷰 수정 성공 테스트")
    @Test
    @WithUserDetails("couple@gmail.com")
    public void update_review_success_test() throws Exception {
        // given
        Long reviewId = 1L;
        ReviewRequest.UpdateDTO request = new ReviewRequest.UpdateDTO("최고의 플래너!");

        String requestBody = om.writeValueAsString(request);

        // when
        ResultActions result = mockMvc.perform(
                MockMvcRequestBuilders
                        .put("/reviews/" + reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        );

        logResult(result);

        // then
        result.andExpect(MockMvcResultMatchers.jsonPath("$.success").value("true"));
    }

    @DisplayName("리뷰 수정 실패 테스트 1 - 존재하지 않는 리뷰")
    @Test
    @WithUserDetails("couple@gmail.com")
    public void update_review_fail_test_review_not_found() throws Exception {
        // given
        Long reviewId = 10L;
        ReviewRequest.UpdateDTO request = new ReviewRequest.UpdateDTO("최고의 플래너!");

        String requestBody = om.writeValueAsString(request);

        // when
        ResultActions result = mockMvc.perform(
                MockMvcRequestBuilders
                        .put("/reviews/" + reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        );

        logResult(result);

        // then
        result.andExpect(MockMvcResultMatchers.jsonPath("$.success").value("false"));
        result.andExpect(MockMvcResultMatchers.jsonPath("$.error.message").value("해당 리뷰가 삭제되었거나 존재하지 않습니다."));
        result.andExpect(MockMvcResultMatchers.jsonPath("$.error.status").value(404));

    }

    @DisplayName("리뷰 수정 실패 테스트 2 - 본인의 리뷰 X")
    @Test
    @WithUserDetails("couple2@gmail.com")
    public void update_review_fail_test_not_my_review() throws Exception {
        // given
        Long reviewId = 1L;
        ReviewRequest.UpdateDTO request = new ReviewRequest.UpdateDTO("최고의 플래너!");

        String requestBody = om.writeValueAsString(request);

        // when
        ResultActions result = mockMvc.perform(
                MockMvcRequestBuilders
                        .put("/reviews/" + reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        );

        logResult(result);

        // then
        result.andExpect(MockMvcResultMatchers.jsonPath("$.success").value("false"));
        result.andExpect(MockMvcResultMatchers.jsonPath("$.error.message").value("사용할 수 없는 기능입니다."));
        result.andExpect(MockMvcResultMatchers.jsonPath("$.error.status").value(403));

    }




    // ============ 리뷰 삭제 테스트 ============
    @DisplayName("리뷰 삭제 성공 테스트")
    @Test
    @WithUserDetails("couple@gmail.com")
    public void delete_review_success_test() throws Exception {
        // given
        Long reviewId = 1L;

        // when
        ResultActions result = mockMvc.perform(
                MockMvcRequestBuilders
                        .delete("/reviews/" + reviewId)
        );

        logResult(result);

        // then
        result.andExpect(MockMvcResultMatchers.jsonPath("$.success").value("true"));
    }


    private void logResult(ResultActions result) throws Exception {
        String responseBody = result.andReturn().getResponse().getContentAsString();
        logger.debug("테스트 : " + responseBody);
    }

    @DisplayName("리뷰 삭제 실패 테스트 1 - 존재하지 않는 리뷰")
    @Test
    @WithUserDetails("couple@gmail.com")
    public void delete_review_fail_test_review_not_found() throws Exception {
        // given
        Long reviewId = 10L;
        ReviewRequest.UpdateDTO request = new ReviewRequest.UpdateDTO("최고의 플래너!");

        String requestBody = om.writeValueAsString(request);

        // when
        ResultActions result = mockMvc.perform(
                MockMvcRequestBuilders
                        .put("/reviews/" + reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        );

        logResult(result);

        // then
        result.andExpect(MockMvcResultMatchers.jsonPath("$.success").value("false"));
        result.andExpect(MockMvcResultMatchers.jsonPath("$.error.message").value("해당 리뷰가 삭제되었거나 존재하지 않습니다."));
        result.andExpect(MockMvcResultMatchers.jsonPath("$.error.status").value(404));

    }

    @DisplayName("리뷰 삭제 실패 테스트 2 - 본인의 리뷰 X")
    @Test
    @WithUserDetails("couple2@gmail.com")
    public void delete_review_fail_test_not_my_review() throws Exception {
        // given
        Long reviewId = 1L;
        ReviewRequest.UpdateDTO request = new ReviewRequest.UpdateDTO("최고의 플래너!");

        String requestBody = om.writeValueAsString(request);

        // when
        ResultActions result = mockMvc.perform(
                MockMvcRequestBuilders
                        .put("/reviews/" + reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        );

        logResult(result);

        // then
        result.andExpect(MockMvcResultMatchers.jsonPath("$.success").value("false"));
        result.andExpect(MockMvcResultMatchers.jsonPath("$.error.message").value("사용할 수 없는 기능입니다."));
        result.andExpect(MockMvcResultMatchers.jsonPath("$.error.status").value(403));

    }
}