package com.example.school.repository;

import java.util.List;

import com.example.school.model.Student;

public interface StudentRepository {

	public List<Student> findAll();

	public Student findById(String id);

	public void save(Student student);

	public void delete(String studentId);

}
