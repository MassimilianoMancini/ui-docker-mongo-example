package com.example.school.controller;

/**
2 * Communicates with a MongoDB server on localhost; start MongoDB with Docker with
3 * docker run -p 27017:27017 --rm mongo:4.4.3
4 */

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.school.model.Student;
import com.example.school.repository.StudentRepository;
import com.example.school.repository.mongo.StudentMongoRepository;
import com.example.school.view.StudentView;
import com.mongodb.MongoClient;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;

import java.util.stream.Collectors;

@ExtendWith(MockitoExtension.class)
public class SchoolControllerRaceConditionIT {

	@Mock
	private StudentView studentView;

	private StudentRepository studentRepository;

	@BeforeEach
	void setUp() {
		MongoClient client = new MongoClient("localhost");
		MongoDatabase database = client.getDatabase("school");
		database.drop();
		MongoCollection<Document> studentCollection = database.getCollection("student");

		// A unique index ensures that the indexed field
		// (in this case "id") does not store duplicate values
		studentCollection.createIndex(Indexes.ascending("id"), new IndexOptions().unique(true));

		studentRepository = new StudentMongoRepository(client);
	}

	@Test
	void testNewStudentConcurrent() {
		Student student = new Student("1", "name");
		// start the threads calling newStudent concurrently
		// on different SchoolController instances, so 'synchronized'
		// methods in the controller will not help
		List<Thread> threads = IntStream.range(0, 10).mapToObj(i -> new Thread(() -> {
			try {
				new SchoolController(studentView, studentRepository).newStudent(student);
			} catch (MongoWriteException e) {
				// E11000 duplicate key error collection:
				// school.student index: id_1 dup key: { id: "1" }
				e.printStackTrace();
			}
		})).peek(t -> t.start()).collect(Collectors.toList());
		// wait for all the threads to finish
		await().atMost(10, TimeUnit.SECONDS).until(() -> threads.stream().noneMatch(t -> t.isAlive()));
		assertThat(studentRepository.findAll()).containsExactly(student);
	}

}
