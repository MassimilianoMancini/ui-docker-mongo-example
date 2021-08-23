package com.example.school.view.swing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.concurrent.TimeUnit;

import org.assertj.swing.core.matcher.JButtonMatcher;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.junit.runner.GUITestRunner;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.MongoDBContainer;

import com.example.school.controller.SchoolController;
import com.example.school.model.Student;
import com.example.school.repository.mongo.StudentMongoRepository;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;

@RunWith(GUITestRunner.class)
public class ModelViewControllerIT extends AssertJSwingJUnitTestCase {

	@ClassRule
	public static final MongoDBContainer mongo = new MongoDBContainer("mongo:5.0.2");

	private MongoClient mongoClient;
	private FrameFixture window;
	private SchoolController schoolController;
	private StudentMongoRepository studentRepository;

	@Override
	protected void onSetUp() {
		mongoClient = new MongoClient(new ServerAddress(mongo.getContainerIpAddress(), mongo.getFirstMappedPort()));
		studentRepository = new StudentMongoRepository(mongoClient);

		// explicit empty database through the repository
		for (Student student : studentRepository.findAll()) {
			studentRepository.delete(student.getId());
		}

		window = new FrameFixture(robot(), GuiActionRunner.execute(() -> {
			StudentSwingView studentSwingView = new StudentSwingView();
			schoolController = new SchoolController(studentSwingView, studentRepository);
			studentSwingView.setSchoolController(schoolController);
			return studentSwingView;
		}));
		window.show();
	}

	@Override
	protected void onTearDown() {
		mongoClient.close();
	}

	@Test
	public void testAddStudent() {
		// use UI to add a student
		window.textBox("idTextBox").enterText("1");
		window.textBox("nameTextBox").enterText("test");
		window.button(JButtonMatcher.withText("Add")).click();
		await().atMost(5, TimeUnit.SECONDS)
				.untilAsserted(() -> assertThat(studentRepository.findById("1")).isEqualTo(new Student("1", "test")));
	}

	@Test
	public void testDeleteStudent() {
		// add student needed for tests
		studentRepository.save(new Student("99", "existing"));
		// use the controller's allStudents to make the student appear in the GUI list
		GuiActionRunner.execute(() -> schoolController.allStudents());
		// select the existing student
		window.list().selectItem(0);
		window.button(JButtonMatcher.withText("Delete Selected")).click();
		// verify that the student has been deleted from the database
		await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> assertThat(studentRepository.findById("99")).isNull());
	}

}
