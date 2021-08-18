package com.example.school.repository.mongo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.example.school.model.Student;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

@Testcontainers
class StudentMongoRepositoryTestcontainersIT {

	@Container
	static final MongoDBContainer mongo = new MongoDBContainer("mongo:5.0.2");

	private static final String STUDENT_COLLECTION_NAME = "student";
	private static final String SCHOOL_DB_NAME = "school";
	private MongoClient client;
	private StudentMongoRepository studentRepository;
	private MongoCollection<Document> studentCollection;

	@BeforeEach
	void setup() {
		client = new MongoClient(new ServerAddress(mongo.getContainerIpAddress(), mongo.getMappedPort(27017)));
		studentRepository = new StudentMongoRepository(client);
		MongoDatabase database = client.getDatabase(SCHOOL_DB_NAME);
		database.drop();
		studentCollection = database.getCollection(STUDENT_COLLECTION_NAME);
	}

	@AfterEach
	void tearDown() {
		client.close();
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
		studentCollection.insertOne(new Document().append("id", id).append("name", name));
	}

	private List<Student> readAllStudentsFromDatabase() {
		return StreamSupport.stream(studentCollection.find().spliterator(), false)
				.map(d -> new Student("" + d.get("id"), "" + d.get("name"))).collect(Collectors.toList());
	}

}
