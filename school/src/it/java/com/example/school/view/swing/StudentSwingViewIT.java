package com.example.school.view.swing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.swing.timing.Pause.pause;
import static org.assertj.swing.timing.Timeout.timeout;
import static org.awaitility.Awaitility.await;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import org.assertj.swing.core.matcher.JButtonMatcher;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.junit.runner.GUITestRunner;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.assertj.swing.timing.Condition;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.example.school.controller.SchoolController;
import com.example.school.model.Student;
import com.example.school.repository.mongo.StudentMongoRepository;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;

import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;

@RunWith(GUITestRunner.class)
public class StudentSwingViewIT extends AssertJSwingJUnitTestCase {
	private static MongoServer server;
	private static InetSocketAddress serverAddress;

	private MongoClient mongoClient;
	private FrameFixture window;
	private StudentSwingView studentSwingView;
	private SchoolController schoolController;
	private StudentMongoRepository studentRepository;
	
	private static final long TIMEOUT = 5000;

	@BeforeClass
	public static void setupServer() {
		server = new MongoServer(new MemoryBackend());
		// bind on a random local port
		serverAddress = server.bind();
	}

	@AfterClass
	public static void shutdownServer() {
		server.shutdown();
	}

	@Override
	protected void onSetUp() throws Exception {
		mongoClient = new MongoClient(new ServerAddress(serverAddress));
		studentRepository = new StudentMongoRepository(mongoClient);
		// explicit empty the database through the repository
		for (Student student : studentRepository.findAll()) {
			studentRepository.delete(student.getId());
		}
		GuiActionRunner.execute(() -> {
			studentSwingView = new StudentSwingView();
			schoolController = new SchoolController(studentSwingView, studentRepository);
			studentSwingView.setSchoolController(schoolController);
			return studentSwingView;
		});
		window = new FrameFixture(robot(), studentSwingView);
		window.show();
	}

	@Override
	protected void onTearDown() {
		mongoClient.close();
	}

	@Test
	public void testAllStudents() {
		// use repository to add students to the database
		Student student1 = new Student("1", "test1");
		Student student2 = new Student("2", "test2");
		studentRepository.save(student1);
		studentRepository.save(student2);
		// use the controller's allStudents
		GuiActionRunner.execute(() -> schoolController.allStudents());
		// and verify that the view's list is populated
		assertThat(window.list().contents()).containsExactly(student1.toString(), student2.toString());
	}
	
	@Test
	public void testAddButtonSuccess() {
		window.textBox("idTextBox").enterText("1");
		window.textBox("nameTextBox").enterText("test");
		window.button(JButtonMatcher.withText("Add")).click();
		await().atMost(5, TimeUnit.SECONDS).untilAsserted(()->
			assertThat(window.list().contents()).containsExactly(new Student("1", "test").toString())
		);
	}
	
	@Test
	public void testAddButtonError() {
		studentRepository.save(new Student("1", "existing"));
		window.textBox("idTextBox").enterText("1");
		window.textBox("nameTextBox").enterText("test");
		window.button(JButtonMatcher.withText("Add")).click();
		
		pause(
			new Condition("Error label to contain text") {
				@Override
				public boolean test() {
					return !window.label("errorMessageLabel").text().trim().isEmpty();
				}
			}
			, timeout(TIMEOUT));
		
		assertThat(window.list().contents()).isEmpty();
		window.label("errorMessageLabel")
			.requireText("Already existing student with id 1: " + (new Student("1", "existing")).toString());
	}
	
	@Test
	public void testDeleteButtonSuccess() {
		// use the controller to populate the view's list...
		GuiActionRunner.execute(()->schoolController.newStudent(new Student("1", "toremove")));
		window.list().selectItem(0);
		window.button(JButtonMatcher.withText("Delete Selected")).click();
		await().atMost(5, TimeUnit.SECONDS).untilAsserted(()->
			assertThat(window.list().contents()).isEmpty()
		);
	}
	
	@Test
	public void testDeleteButtonError() {
		// manually add a student to the list, which will not be in the db
		Student student = new Student("1", "non existent");
		GuiActionRunner.execute(()->studentSwingView.getListStudentsModel().addElement(student));
		window.list().selectItem(0);
		window.button(JButtonMatcher.withText("Delete Selected")).click();
		await().atMost(5,  TimeUnit.SECONDS).untilAsserted(()->
			window.label("errorMessageLabel").requireText("No existing student with id 1: " + student.toString())
		);
		assertThat(window.list().contents()).containsExactly(student.toString());
	}

}
