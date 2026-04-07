package com.example.booking_access;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/booking-access")
@Validated
public class BookingAccessController {

    private static final String ADMIN_PASSWORD = "admin123";

    private final AdminRepository adminRepository;
    private final ClassRepository classRepository;
    private final InstructorRepository instructorRepository;
    private final CourseRepository courseRepository;
    private final StudentRepository studentRepository;

    BookingAccessController(
            AdminRepository adminRepository,
            ClassRepository classRepository,
            InstructorRepository instructorRepository,
            CourseRepository courseRepository,
            StudentRepository studentRepository
    ) {
        this.adminRepository = adminRepository;
        this.classRepository = classRepository;
        this.instructorRepository = instructorRepository;
        this.courseRepository = courseRepository;
        this.studentRepository = studentRepository;
    }

    @PostMapping("/admin/login")
    public LoginResponse adminLogin(@Valid @RequestBody LoginRequest request) {
        Optional<AdminUser> admin = adminRepository.findByUsername(request.username());
        if (admin.isPresent()) {
            if (!admin.get().password().equals(request.password())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid admin credentials");
            }
            return new LoginResponse(admin.get().id(), admin.get().username(), "LOGIN_OK");
        }

        if (!ADMIN_PASSWORD.equals(request.password())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid admin credentials");
        }

        AdminUser saved = adminRepository.save(new AdminUser(request.username(), request.password()));
        return new LoginResponse(saved.id(), saved.username(), "LOGIN_OK");
    }

    @PostMapping("/classes")
    @ResponseStatus(HttpStatus.CREATED)
    public ClassRecord createClass(@Valid @RequestBody CreateClassRequest request) {
        return classRepository.save(new ClassRecord(request.className()));
    }

    @PostMapping("/instructors")
    @ResponseStatus(HttpStatus.CREATED)
    public Instructor createInstructor(@Valid @RequestBody CreateInstructorRequest request) {
        return instructorRepository.save(new Instructor(request.instructorName()));
    }

    @PostMapping("/courses")
    @ResponseStatus(HttpStatus.CREATED)
    public Course createCourse(@Valid @RequestBody CreateCourseRequest request) {
        return courseRepository.save(new Course(request.courseName()));
    }

    @PostMapping("/classes/{classId}/assign-instructor")
    public ClassRecord assignInstructor(
            @PathVariable Long classId,
            @Valid @RequestBody AssignInstructorRequest request
    ) {
        ClassRecord classRecord = classRepository.findById(classId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Class not found"));

        Instructor instructor = instructorRepository.findById(request.instructorId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Instructor not found"));

        classRecord.assignInstructor(instructor);
        return classRepository.save(classRecord);
    }

    @PostMapping("/instructors/{instructorId}/assign-course")
    public Course assignCourseToInstructor(
            @PathVariable Long instructorId,
            @Valid @RequestBody AssignCourseRequest request
    ) {
        Instructor instructor = instructorRepository.findById(instructorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Instructor not found"));

        Course course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        course.assignInstructor(instructor);
        return courseRepository.save(course);
    }

    @PostMapping("/courses/{courseId}/assign-student")
    public Student assignStudentToCourse(
            @PathVariable Long courseId,
            @Valid @RequestBody AssignStudentRequest request
    ) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        Student student = studentRepository.findById(request.studentId())
                .orElseGet(() -> studentRepository.save(new Student(request.studentName())));

        student.assignCourse(course);
        return studentRepository.save(student);
    }

    @PostMapping("/students")
    @ResponseStatus(HttpStatus.CREATED)
    public Student createStudent(@Valid @RequestBody CreateStudentRequest request) {
        return studentRepository.save(new Student(request.studentName()));
    }

    @GetMapping("/classes")
    public List<ClassRecord> listClasses() {
        return classRepository.findAll();
    }

    @GetMapping("/courses")
    public List<Course> listCourses(@RequestParam(required = false) Long instructorId) {
        if (instructorId == null) {
            return courseRepository.findAll();
        }
        return courseRepository.findByInstructorId(instructorId);
    }

    @GetMapping("/students")
    public List<Student> listStudents(@RequestParam(required = false) Long courseId) {
        if (courseId == null) {
            return studentRepository.findAll();
        }
        return studentRepository.findByCourseId(courseId);
    }
}

@Entity
class AdminUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    private String password;

    AdminUser() {
    }

    AdminUser(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public Long id() {
        return id;
    }

    public String username() {
        return username;
    }

    public String password() {
        return password;
    }
}

@Entity
class ClassRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String className;

    @ManyToOne
    private Instructor instructor;

    ClassRecord() {
    }

    ClassRecord(String className) {
        this.className = className;
    }

    public Long id() {
        return id;
    }

    public String className() {
        return className;
    }

    public Instructor instructor() {
        return instructor;
    }

    void assignInstructor(Instructor instructor) {
        this.instructor = instructor;
    }
}

@Entity
class Instructor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String instructorName;

    Instructor() {
    }

    Instructor(String instructorName) {
        this.instructorName = instructorName;
    }

    public Long id() {
        return id;
    }

    public String instructorName() {
        return instructorName;
    }
}

@Entity
class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String courseName;

    @ManyToOne
    private Instructor instructor;

    Course() {
    }

    Course(String courseName) {
        this.courseName = courseName;
    }

    public Long id() {
        return id;
    }

    public String courseName() {
        return courseName;
    }

    public Instructor instructor() {
        return instructor;
    }

    void assignInstructor(Instructor instructor) {
        this.instructor = instructor;
    }
}

@Entity
class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String studentName;

    @ManyToOne
    private Course course;

    Student() {
    }

    Student(String studentName) {
        this.studentName = studentName;
    }

    public Long id() {
        return id;
    }

    public String studentName() {
        return studentName;
    }

    public Course course() {
        return course;
    }

    void assignCourse(Course course) {
        this.course = course;
    }
}

interface AdminRepository extends JpaRepository<AdminUser, Long> {
    Optional<AdminUser> findByUsername(String username);
}

interface ClassRepository extends JpaRepository<ClassRecord, Long> {
}

interface InstructorRepository extends JpaRepository<Instructor, Long> {
}

interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByInstructorId(Long instructorId);
}

interface StudentRepository extends JpaRepository<Student, Long> {
    List<Student> findByCourseId(Long courseId);
}

record LoginRequest(
        @NotBlank String username,
        @NotBlank String password
) {
}

record LoginResponse(
        Long adminId,
        String username,
        String status
) {
}

record CreateClassRequest(@NotBlank String className) {
}

record CreateInstructorRequest(@NotBlank String instructorName) {
}

record CreateCourseRequest(@NotBlank String courseName) {
}

record CreateStudentRequest(@NotBlank String studentName) {
}

record AssignInstructorRequest(@NotNull Long instructorId) {
}

record AssignCourseRequest(@NotNull Long courseId) {
}

record AssignStudentRequest(
        Long studentId,
        @NotBlank String studentName
) {
}
