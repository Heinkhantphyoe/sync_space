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

	@Test
	void taskAssignees() throws Exception {
		register("ada.assignees@example.com", "Ada");
		register("grace.assignees@example.com", "Grace");
		register("outsider.assignees@example.com", "Outsider");
		String ada = login("ada.assignees@example.com");
		String grace = login("grace.assignees@example.com");
		String outsider = login("outsider.assignees@example.com");
		String adaId = userId(ada);
		String graceId = userId(grace);
		String outsiderId = userId(outsider);

		MvcResult created = mvc.perform(post("/api/spaces")
						.header("Authorization", "Bearer " + ada)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Assignees\"}"))
				.andExpect(status().isCreated())
				.andReturn();
		String spaceId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
		mvc.perform(post("/api/spaces/" + spaceId + "/members")
						.header("Authorization", "Bearer " + ada)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"grace.assignees@example.com\"}"))
				.andExpect(status().isOk());

		String board = mvc.perform(get("/api/spaces/" + spaceId + "/board").header("Authorization", "Bearer " + ada))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		String todoId = JsonPath.read(board, "$.columns[0].id");
		String withTask = mvc.perform(post("/api/spaces/" + spaceId + "/tasks")
						.header("Authorization", "Bearer " + ada)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"columnId\":\"" + todoId + "\",\"title\":\"Write spec\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.columns[0].tasks[0].assignees.length()").value(0))
				.andReturn()
				.getResponse()
				.getContentAsString();
		String taskId = JsonPath.read(withTask, "$.columns[0].tasks[0].id");

		mvc.perform(patch("/api/spaces/" + spaceId + "/tasks/" + taskId + "/assignees")
						.header("Authorization", "Bearer " + grace)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"userIds\":[\"" + graceId + "\",\"" + adaId + "\",\"" + adaId + "\"]}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.columns[0].tasks[0].assignees.length()").value(2))
				.andExpect(jsonPath("$.columns[0].tasks[0].assignees[0].displayName").value("Ada"))
				.andExpect(jsonPath("$.columns[0].tasks[0].assignees[0].userId").value(adaId))
				.andExpect(jsonPath("$.columns[0].tasks[0].assignees[1].displayName").value("Grace"));

		mvc.perform(patch("/api/spaces/" + spaceId + "/tasks/" + taskId + "/assignees")
						.header("Authorization", "Bearer " + ada)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"userIds\":[\"" + adaId + "\",\"" + graceId + "\"]}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.columns[0].tasks[0].assignees.length()").value(2));

		mvc.perform(patch("/api/spaces/" + spaceId + "/tasks/" + taskId + "/assignees")
						.header("Authorization", "Bearer " + ada)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"userIds\":[\"" + outsiderId + "\"]}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Only people in this space can be assigned"));

		mvc.perform(patch("/api/spaces/" + spaceId + "/tasks/" + taskId + "/assignees")
						.header("Authorization", "Bearer " + outsider)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"userIds\":[\"" + adaId + "\"]}"))
				.andExpect(status().isForbidden());

		mvc.perform(patch("/api/spaces/" + spaceId + "/tasks/" + taskId + "/assignees")
						.header("Authorization", "Bearer " + ada)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"userIds\":[\"" + adaId + "\"]}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.columns[0].tasks[0].assignees.length()").value(1))
				.andExpect(jsonPath("$.columns[0].tasks[0].assignees[0].displayName").value("Ada"));

		mvc.perform(get("/api/spaces/" + spaceId + "/tasks/" + taskId + "/activity")
						.header("Authorization", "Bearer " + grace))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.kind == 'ASSIGNED')].summary", hasItem("assigned Ada")))
				.andExpect(jsonPath("$[?(@.kind == 'ASSIGNED')].summary", hasItem("assigned Grace")))
				.andExpect(jsonPath("$[?(@.kind == 'UNASSIGNED')].summary", hasItem("unassigned Grace")));

		mvc.perform(delete("/api/spaces/" + spaceId + "/members/" + graceId).header("Authorization", "Bearer " + ada))
				.andExpect(status().isOk());
		mvc.perform(patch("/api/spaces/" + spaceId + "/tasks/" + taskId + "/assignees")
						.header("Authorization", "Bearer " + ada)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"userIds\":[\"" + graceId + "\"]}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Only people in this space can be assigned"));

		mvc.perform(post("/api/spaces/" + spaceId + "/members")
						.header("Authorization", "Bearer " + ada)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"grace.assignees@example.com\"}"))
				.andExpect(status().isOk());
		mvc.perform(patch("/api/spaces/" + spaceId + "/tasks/" + taskId + "/assignees")
						.header("Authorization", "Bearer " + ada)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"userIds\":[\"" + adaId + "\",\"" + graceId + "\"]}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.columns[0].tasks[0].assignees.length()").value(2));
		mvc.perform(delete("/api/spaces/" + spaceId + "/members/" + graceId).header("Authorization", "Bearer " + ada))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.columns[0].tasks[0].assignees.length()").value(1))
				.andExpect(jsonPath("$.columns[0].tasks[0].assignees[0].displayName").value("Ada"));
		mvc.perform(get("/api/spaces/" + spaceId + "/tasks/" + taskId + "/activity")
						.header("Authorization", "Bearer " + ada))
				.andExpect(jsonPath("$[?(@.kind == 'UNASSIGNED')].summary", hasItem("unassigned Grace")));
	}

	@Test
	void taskLabelsPriorityAndSearchFields() throws Exception {
		register("ada.labels@example.com", "Ada");
		register("grace.labels@example.com", "Grace");
		register("outsider.labels@example.com", "Outsider");
		String ada = login("ada.labels@example.com");
		String grace = login("grace.labels@example.com");
		String outsider = login("outsider.labels@example.com");

		MvcResult created = mvc.perform(post("/api/spaces")
						.header("Authorization", "Bearer " + ada)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Labels\"}"))
				.andExpect(status().isCreated())
				.andReturn();
		String spaceId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
		mvc.perform(post("/api/spaces/" + spaceId + "/members")
						.header("Authorization", "Bearer " + ada)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"grace.labels@example.com\"}"))
				.andExpect(status().isOk());

		String board = mvc.perform(get("/api/spaces/" + spaceId + "/board").header("Authorization", "Bearer " + ada))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		String todoId = JsonPath.read(board, "$.columns[0].id");
		String withTask = mvc.perform(post("/api/spaces/" + spaceId + "/tasks")
						.header("Authorization", "Bearer " + ada)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"columnId\":\"" + todoId + "\",\"title\":\"Fix login\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.columns[0].tasks[0].priority").value("MEDIUM"))
				.andExpect(jsonPath("$.columns[0].tasks[0].labels.length()").value(0))
				.andReturn()
				.getResponse()
				.getContentAsString();
		String taskId = JsonPath.read(withTask, "$.columns[0].tasks[0].id");

		mvc.perform(patch("/api/spaces/" + spaceId + "/tasks/" + taskId + "/labels")
						.header("Authorization", "Bearer " + outsider)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"labels\":[\"BUG\"]}"))
				.andExpect(status().isForbidden());
		mvc.perform(patch("/api/spaces/" + spaceId + "/tasks/" + taskId + "/labels")
						.header("Authorization", "Bearer " + ada)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"labels\":[\"NOPE\"]}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Invalid request"));
		mvc.perform(patch("/api/spaces/" + spaceId + "/tasks/" + taskId + "/labels")
						.header("Authorization", "Bearer " + ada)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Labels are required"));

		mvc.perform(patch("/api/spaces/" + spaceId + "/tasks/" + taskId + "/labels")
						.header("Authorization", "Bearer " + grace)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"labels\":[\"DESIGN\",\"BUG\",\"BUG\"]}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.columns[0].tasks[0].labels.length()").value(2))
				.andExpect(jsonPath("$.columns[0].tasks[0].labels[0]").value("BUG"))
				.andExpect(jsonPath("$.columns[0].tasks[0].labels[1]").value("DESIGN"));
		mvc.perform(patch("/api/spaces/" + spaceId + "/tasks/" + taskId + "/labels")
						.header("Authorization", "Bearer " + grace)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"labels\":[\"BUG\",\"DESIGN\"]}"))
				.andExpect(status().isOk());

		mvc.perform(patch("/api/spaces/" + spaceId + "/tasks/" + taskId + "/priority")
						.header("Authorization", "Bearer " + outsider)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"priority\":\"HIGH\"}"))
				.andExpect(status().isForbidden());
		mvc.perform(patch("/api/spaces/" + spaceId + "/tasks/" + taskId + "/priority")
						.header("Authorization", "Bearer " + ada)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"priority\":\"URGENT\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Invalid request"));
		mvc.perform(patch("/api/spaces/" + spaceId + "/tasks/" + taskId + "/priority")
						.header("Authorization", "Bearer " + ada)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"priority\":\"HIGH\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.columns[0].tasks[0].priority").value("HIGH"));
		mvc.perform(patch("/api/spaces/" + spaceId + "/tasks/" + taskId + "/priority")
						.header("Authorization", "Bearer " + ada)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"priority\":\"HIGH\"}"))
				.andExpect(status().isOk());

		mvc.perform(patch("/api/spaces/" + spaceId + "/tasks/" + taskId + "/labels")
						.header("Authorization", "Bearer " + ada)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"labels\":[\"FEATURE\"]}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.columns[0].tasks[0].labels.length()").value(1))
				.andExpect(jsonPath("$.columns[0].tasks[0].labels[0]").value("FEATURE"));

		mvc.perform(get("/api/spaces/" + spaceId + "/tasks/" + taskId + "/activity")
						.header("Authorization", "Bearer " + ada))
				.andExpect(jsonPath("$.length()").value(7))
				.andExpect(jsonPath("$[?(@.kind == 'LABELED')].summary", hasItem("added the Bug label")))
				.andExpect(jsonPath("$[?(@.kind == 'LABELED')].summary", hasItem("added the Design label")))
				.andExpect(jsonPath("$[?(@.kind == 'LABELED')].summary", hasItem("added the Feature label")))
				.andExpect(jsonPath("$[?(@.kind == 'UNLABELED')].summary", hasItem("removed the Bug label")))
				.andExpect(jsonPath("$[?(@.kind == 'UNLABELED')].summary", hasItem("removed the Design label")))
				.andExpect(jsonPath("$[?(@.kind == 'PRIORITY_CHANGED')].summary", hasItem("set priority to High")));

		mvc.perform(patch("/api/spaces/" + spaceId + "/tasks/" + taskId)
						.header("Authorization", "Bearer " + ada)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"Fix login\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.columns[0].tasks[0].priority").value("HIGH"))
				.andExpect(jsonPath("$.columns[0].tasks[0].labels[0]").value("FEATURE"))
				.andExpect(jsonPath("$.columns[0].tasks[0].assignees.length()").value(0));

		String adaId = userId(ada);
		mvc.perform(patch("/api/spaces/" + spaceId + "/tasks/" + taskId)
						.header("Authorization", "Bearer " + ada)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"Fix the login\",\"description\":\"Session\",\"userIds\":[\"" + adaId
								+ "\"],\"labels\":[\"BUG\"],\"priority\":\"LOW\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.columns[0].tasks[0].title").value("Fix the login"))
				.andExpect(jsonPath("$.columns[0].tasks[0].description").value("Session"))
				.andExpect(jsonPath("$.columns[0].tasks[0].priority").value("LOW"))
				.andExpect(jsonPath("$.columns[0].tasks[0].labels[0]").value("BUG"))
				.andExpect(jsonPath("$.columns[0].tasks[0].assignees[0].displayName").value("Ada"));
		mvc.perform(get("/api/spaces/" + spaceId + "/tasks/" + taskId + "/activity")
						.header("Authorization", "Bearer " + ada))
				.andExpect(jsonPath("$.length()").value(13))
				.andExpect(jsonPath("$[?(@.kind == 'RENAMED')].summary", hasItem("renamed this to Fix the login")))
				.andExpect(jsonPath("$[?(@.kind == 'ASSIGNED')].summary", hasItem("assigned Ada")))
				.andExpect(jsonPath("$[?(@.kind == 'UNLABELED')].summary", hasItem("removed the Feature label")))
				.andExpect(jsonPath("$[?(@.kind == 'LABELED')].summary", hasItem("added the Bug label")))
				.andExpect(jsonPath("$[?(@.kind == 'PRIORITY_CHANGED')].summary", hasItem("set priority to Low")));

		mvc.perform(delete("/api/spaces/" + spaceId).header("Authorization", "Bearer " + ada))
				.andExpect(status().isNoContent());
	}

	private void register(String email, String name) throws Exception {
		mvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + email + "\",\"password\":\"password1\",\"displayName\":\"" + name + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isNotEmpty());
	}

	private String userId(String token) throws Exception {
		MvcResult result = mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
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
