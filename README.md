## About This Project

This repository holds the code for Learn Mate, a web-based educational platform that was my main project for the Software Engineering module at SLIIT. The goal wasn't just to build an application, but to put software engineering principles into practice—moving from requirements to a fully functional, structured system.

### The Core Idea: Role-Based Access

The heart of this application is its system for handling different types of users. I designed and built three distinct portals to create a secure and tailored experience for everyone:

*   **Administrator Portal:** Has a global view of the system to manage users and core settings.
*   **Teacher Portal:** Allows teachers to manage the courses they teach, view their enrolled students, and update marks.
*   **Student Portal:** Gives students a simple interface to view their enrolled courses and check their grades.

The main technical challenge was architecting the backend to enforce these permissions cleanly, ensuring that data and features were only accessible to the correct user roles.

### Tech Stack

*   **Backend:** Java with Spring Boot
*   **Frontend:** Thymeleaf for server-side HTML rendering
*   **Database:** MySQL
*   **Build Tool:** Maven

### Key Learnings

This project was a fantastic exercise in backend architecture. It was a deep dive into translating user stories into database schemas and secure API endpoints. While it's a university project and not a production-ready system, it was an invaluable experience in building a structured, maintainable application from the ground up.
