# Connect - Real-time Chat Application with AI Assistance

[![Java](https://img.shields.io/badge/Java-21+-orange)](https://www.oracle.com/java/technologies/javase-jdk21-downloads.html)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.1.0-brightgreen)](https://spring.io/projects/spring-boot)
[![JavaFX](https://img.shields.io/badge/JavaFX-21-blue)](https://openjfx.io/)
[![MongoDB](https://img.shields.io/badge/MongoDB-4.4+-green)](https://www.mongodb.com/)

## Overview

Connect is a robust, full-stack desktop chat application designed for seamless real-time communication enhanced by AI assistance. This project showcases proficiency in full-stack Java development, real-time communication protocols, and integration with external APIs. It utilizes JavaFX for a rich desktop user interface, Spring Boot for a scalable and maintainable backend, WebSockets for instant messaging, and Google's Gemini API for intelligent, context-aware responses.

## Key Features

*   **Real-time Bi-directional Messaging:** Experience instantaneous message delivery and updates using WebSockets, ensuring seamless communication.
*   **AI-Powered Assistance:** Leverage the power of Google's Gemini API to receive intelligent responses, suggestions, and assistance within the chat interface.
*   **User Authentication:** Secure user accounts with robust signup and login functionality, protecting user data and privacy.
*   **Message Persistence:** Messages are stored persistently using MongoDB, ensuring no data loss and allowing users to access their chat history.
*   **User-Friendly Interface:** Enjoy an intuitive and responsive desktop interface built with JavaFX, featuring a modern dark theme for enhanced usability and visual appeal.

## Tech Stack

*   **Frontend:** JavaFX
*   **Backend:** Spring Boot (Java)
*   **Database:** MongoDB
*   **Real-time Communication:** WebSockets
*   **AI Integration:** Google Gemini API
*   **Build Tool:** Maven

## Architecture

Connect employs a layered architecture to promote modularity, maintainability, and scalability:

*   **Presentation Layer (Frontend - JavaFX):** Responsible for user interaction, rendering the user interface, and handling communication with the backend.
*   **Application Layer (Backend - Spring Boot):** Contains the business logic, manages data access, and orchestrates interactions between different services.
*   **Data Access Layer (Backend - Spring Data MongoDB):** Handles database interactions, providing an abstraction layer for data persistence.
*   **External API Integration (Backend):** Integrates with Google's Gemini API for AI-powered responses.

## Setup and Installation

### Prerequisites

*   Java 21+ JDK
*   Apache Maven
*   MongoDB (running locally or a cloud instance)
*   A Google Cloud Project with the Gemini API enabled and a valid API key.

### Backend Setup

1.  Navigate to the `connect-backend` directory: `cd connect-backend`
2.  Create a file named `application.properties` in `src/main/resources`.
3.  Configure the following properties:

    ```properties
    spring.data.mongodb.uri=mongodb://localhost:27017/chatapp  # Your MongoDB connection string
    server.port=8080                                         # Backend server port
    gemini.api.key=YOUR_GEMINI_API_KEY                     # Your Google Gemini API key
    ```

### Frontend Setup

1.  Navigate to the `connect` directory: `cd connect`

### Build and Run

1.  Start the backend server: `cd connect-backend && mvn spring-boot:run`
2.  Start the frontend application: `cd connect && mvn javafx:run`

## Future Enhancements

*   Private messaging functionality
*   File sharing capabilities
*   Enhanced message history management
*   User profiles with customizable avatars
*   Rich text formatting within messages
*   Mobile application development

## Contributing

Contributions are welcome! Please fork the repository and submit pull requests.

