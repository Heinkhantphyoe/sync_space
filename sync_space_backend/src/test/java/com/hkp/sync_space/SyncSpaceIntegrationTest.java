package com.hkp.sync_space;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

@SpringBootTest(properties = "app.embedded-postgres=true")
@AutoConfigureMockMvc
class SyncSpaceIntegrationTest {

	@Autowired
	private MockMvc mvc;

	@Test
	void registerLoginMembershipAndTaskMove() throws Exception {
		register("ada@example.com", "Ada");
		register("grace@example.com", "Grace");
		String ada = login("ada@example.com");
		String grace = login("grace@example.com");

		mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + ada))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("ada@example.com"))
				.andExpect(jsonPath("$.displayName").value("Ada"));

		MvcResult created = mvc.perform(post("/api/spaces")
						.header("Authorization", "Bearer " + ada)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Launch\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.role").value("OWNER"))
				.andReturn();
		String spaceId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

		mvc.perform(get("/api/spaces/" + spaceId + "/board").header("Authorization", "Bearer " + grace))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("You are not a member of this space"));

		mvc.perform(post("/api/spaces/" + spaceId + "/members")
						.header("Authorization", "Bearer " + ada)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"grace@example.com\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.members[1].email").value("grace@example.com"))
				.andExpect(jsonPath("$.members[1].role").value("MEMBER"));

		String board = mvc.perform(get("/api/spaces/" + spaceId + "/board").header("Authorization", "Bearer " + grace))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.columns[0].name").value("To Do"))
				.andExpect(jsonPath("$.columns[1].name").value("In Progress"))
				.andExpect(jsonPath("$.columns[2].name").value("Done"))
				.andReturn()
				.getResponse()
				.getContentAsString();
		String todoId = JsonPath.read(board, "$.columns[0].id");
		String progressId = JsonPath.read(board, "$.columns[1].id");

		mvc.perform(post("/api/spaces/" + spaceId + "/tasks")
						.header("Authorization", "Bearer " + ada)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"columnId\":\"" + todoId + "\",\"title\":\"Write spec\"}"))
				.andExpect(status().isOk());
		String withTasks = mvc.perform(post("/api/spaces/" + spaceId + "/tasks")
						.header("Authorization", "Bearer " + ada)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"columnId\":\"" + todoId + "\",\"title\":\"Ship it\",\"description\":\"Release the board\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.columns[0].tasks[0].title").value("Write spec"))
				.andExpect(jsonPath("$.columns[0].tasks[0].position").value(0))
				.andExpect(jsonPath("$.columns[0].tasks[1].title").value("Ship it"))
				.andExpect(jsonPath("$.columns[0].tasks[1].position").value(1))
				.andReturn()
				.getResponse()
				.getContentAsString();
		String writeSpecId = JsonPath.read(withTasks, "$.columns[0].tasks[0].id");

		mvc.perform(post("/api/spaces/" + spaceId + "/tasks/" + writeSpecId + "/move")
						.header("Authorization", "Bearer " + ada)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"toColumnId\":\"" + progressId + "\",\"toIndex\":0}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.columns[0].tasks[0].title").value("Ship it"))
				.andExpect(jsonPath("$.columns[0].tasks[0].position").value(0))
				.andExpect(jsonPath("$.columns[1].tasks[0].title").value("Write spec"))
				.andExpect(jsonPath("$.columns[1].tasks[0].position").value(0))
				.andExpect(jsonPath("$.columns[1].tasks[0].columnId").value(progressId));

		mvc.perform(get("/api/spaces/" + spaceId + "/board").header("Authorization", "Bearer " + grace))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.columns[0].tasks[0].title").value("Ship it"))
				.andExpect(jsonPath("$.columns[1].tasks[0].title").value("Write spec"))
				.andExpect(jsonPath("$.columns[1].tasks[0].position").value(0));

		mvc.perform(delete("/api/spaces/" + spaceId).header("Authorization", "Bearer " + grace))
				.andExpect(status().isForbidden());
		mvc.perform(patch("/api/spaces/" + spaceId)
						.header("Authorization", "Bearer " + ada)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Launch plan\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.spaceName").value("Launch plan"));
	}

	@Test
	void taskCommentsAndActivity() throws Exception {
		register("ada.activity@example.com", "Ada");
		register("grace.activity@example.com", "Grace");
		register("outsider.activity@example.com", "Outsider");
		String ada = login("ada.activity@example.com");
		String grace = login("grace.activity@example.com");
		String outsider = login("outsider.activity@example.com");

		MvcResult created = mvc.perform(post("/api/spaces")
						.header("Authorization", "Bearer " + ada)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Activity\"}"))
				.andExpect(status().isCreated())
				.andReturn();
		String spaceId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
		mvc.perform(post("/api/spaces/" + spaceId + "/members")
						.header("Authorization", "Bearer " + ada)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"grace.activity@example.com\"}"))
				.andExpect(status().isOk());

		String board = mvc.perform(get("/api/spaces/" + spaceId + "/board").header("Authorization", "Bearer " + ada))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		String todoId = JsonPath.read(board, "$.columns[0].id");
		String progressId = JsonPath.read(board, "$.columns[1].id");

		String withTask = mvc.perform(post("/api/spaces/" + spaceId + "/tasks")
						.header("Authorization", "Bearer " + ada)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"columnId\":\"" + todoId + "\",\"title\":\"Write spec\"}"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		String taskId = JsonPath.read(withTask, "$.columns[0].tasks[0].id");

		mvc.perform(get("/api/spaces/" + spaceId + "/tasks/" + taskId + "/activity")
						.header("Authorization", "Bearer " + ada))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].kind").value("CREATED"))
				.andExpect(jsonPath("$[0].actorName").value("Ada"))
				.andExpect(jsonPath("$[0].summary").value("created this task"));

		mvc.perform(post("/api/spaces/" + spaceId + "/tasks/" + taskId + "/move")
						.header("Authorization", "Bearer " + ada)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"toColumnId\":\"" + todoId + "\",\"toIndex\":0}"))
				.andExpect(status().isOk());
		mvc.perform(get("/api/spaces/" + spaceId + "/tasks/" + taskId + "/activity")
						.header("Authorization", "Bearer " + ada))
				.andExpect(jsonPath("$.length()").value(1));

		mvc.perform(post("/api/spaces/" + spaceId + "/tasks/" + taskId + "/move")
						.header("Authorization", "Bearer " + ada)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"toColumnId\":\"" + progressId + "\",\"toIndex\":0}"))
				.andExpect(status().isOk());
		mvc.perform(patch("/api/spaces/" + spaceId + "/tasks/" + taskId)
						.header("Authorization", "Bearer " + ada)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"Write the spec\",\"description\":\"Notes\"}"))
				.andExpect(status().isOk());
		mvc.perform(patch("/api/spaces/" + spaceId + "/tasks/" + taskId)
						.header("Authorization", "Bearer " + ada)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"Write the spec\",\"description\":\"Notes\"}"))
				.andExpect(status().isOk());

		mvc.perform(post("/api/spaces/" + spaceId + "/tasks/" + taskId + "/comments")
						.header("Authorization", "Bearer " + grace)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"body\":\"Looks good\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(5))
				.andExpect(jsonPath("$[?(@.kind == 'MOVED')].summary", hasItem("moved this to In Progress")))
				.andExpect(jsonPath("$[?(@.kind == 'RENAMED')].summary", hasItem("renamed this to Write the spec")))
				.andExpect(jsonPath("$[?(@.kind == 'DESCRIPTION_CHANGED')].summary", hasItem("updated the description")))
				.andExpect(jsonPath("$[?(@.kind == 'COMMENT')].body", hasItem("Looks good")))
				.andExpect(jsonPath("$[?(@.kind == 'COMMENT')].actorName", hasItem("Grace")));

		mvc.perform(post("/api/spaces/" + spaceId + "/tasks/" + taskId + "/comments")
						.header("Authorization", "Bearer " + ada)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"body\":\"   \"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Comment is required"));

		mvc.perform(get("/api/spaces/" + spaceId + "/tasks/" + taskId + "/activity")
						.header("Authorization", "Bearer " + outsider))
				.andExpect(status().isForbidden());
	}

	private void register(String email, String name) throws Exception {
		mvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + email + "\",\"password\":\"password1\",\"displayName\":\"" + name + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isNotEmpty());
	}

	private String login(String email) throws Exception {
		MvcResult result = mvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + email + "\",\"password\":\"password1\"}"))
				.andExpect(status().isOk())
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
	}

}
