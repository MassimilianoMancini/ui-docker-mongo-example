package com.example.school.repository.mongo;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetSocketAddress;
import java.util.stream.StreamSupport;
import java.util.List;
import java.util.stream.Collectors;

import org.bson.Document;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.school.model.Student;
import com.example.school.repository.StudentRepository;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;

class StudentMongoRepositoryTest {

	private static final String STUDENT_COLLECTION_NAME = "student";
	private static final String SCHOOL_DB_NAME = "school";
	private static MongoServer server;
	private static InetSocketAddress serverAddress;
	private MongoCollection<Document> studentCollection;
	private MongoClient client;
	private StudentRepository studentRepository;

	@BeforeAll
	static void setupServer() {
		server = new MongoServer(new MemoryBackend());
		serverAddress = server.bind();
	}

	@BeforeEach
	void setup() {
		client = new MongoClient(new ServerAddress(serverAddress));
		studentRepository = new StudentMongoRepository(client);
		MongoDatabase database = client.getDatabase(SCHOOL_DB_NAME);
		database.drop();
		studentCollection = database.getCollection(STUDENT_COLLECTION_NAME);

	}

	@Test
	void testFindAllWhenDatabaseIsEmpty() {
		assertThat(studentRepository.findAll()).isEmpty();
	}

	@Test
	void tistFindAllWhenDatabaseIsNotEmpty() {
		addTestStudentToDatabase("1", "test1");
		addTestStudentToDatabase("2", "test2");
		assertThat(studentRepository.findAll()).containsExactly(new Student("1", "test1"), new Student("2", "test2"));
	}
	
	@Test
	void testFindByIdNotFound() {
		assertThat(studentRepository.findById("1")).isNull();
	}
	
	@Test
	void testFindByIdFound() {
		addTestStudentToDatabase("1", "test1");
		addTestStudentToDatabase("2", "test2");
		assertThat(studentRepository.findById("2")).isEqualTo(new Student("2", "test2"));
	}
	
	@Test
	void testSave() {
		Student student = new Student("1", "added student");
		studentRepository.save(student);
		assertThat(readAllStudentsFromDatabase()).containsExactly(student);
	}
	
	@Test
	void testDelete() {
		addTestStudentToDatabase("1", "test1");
		addTestStudentToDatabase("2", "test2");
		studentRepository.delete("1");
		assertThat(studentRepository.findAll()).containsExactly(new Student("2", "test2"));
	}

	private void addTestStudentToDatabase(String id, String name) {
		studentCollection.insertOne(
		  new Document()
		  .append("id", id)
		  .append("name", name));
	}
	
	private List<Student> readAllStudentsFromDatabase() {
		return StreamSupport.stream(studentCollection.find().spliterator(), false)
		         .map(d->new Student("" + d.get("id"), "" + d.get("name")))
		         .collect(Collectors.toList());
	}

}
