package com.example.school.repository.mongo;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.bson.Document;

import com.example.school.model.Student;
import com.example.school.repository.StudentRepository;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;

public class StudentMongoRepository implements StudentRepository {

	private static final String STUDENT_COLLECTION_NAME = "student";
	private static final String SCHOOL_DB_NAME = "school";
	private MongoCollection<Document> studentCollection;

	public StudentMongoRepository(MongoClient client) {
		studentCollection = client.getDatabase(SCHOOL_DB_NAME).getCollection(STUDENT_COLLECTION_NAME);
	}

	@Override
	public List<Student> findAll() {
		return StreamSupport.stream(studentCollection.find().spliterator(), false).map(this::fromDocumentToStudent)
				.collect(Collectors.toList());
	}

	@Override
	public Student findById(String id) {
		Document d = studentCollection.find(Filters.eq("id", id)).first();
		if (d != null) {
			return fromDocumentToStudent(d);
		}
		return null;
	}

	@Override
	public void save(Student student) {
		studentCollection.insertOne(new Document().append("id", student.getId()).append("name", student.getName()));
	}

	@Override
	public void delete(String studentId) {
		studentCollection.deleteOne(Filters.eq("id", studentId));

	}

	private Student fromDocumentToStudent(Document d) {
		return new Student("" + d.get("id"), "" + d.get("name"));
	}
}
