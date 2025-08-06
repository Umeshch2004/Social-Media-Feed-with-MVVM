Java Social Media Feed - MVVM Architecture
==========================================

A Twitter-like social media feed application demonstrating MVVM architecture, reactive programming, and plugin system in Java.

Features
--------

*   MVVM Architecture with clean separation of concerns
    
*   Reactive Programming using Java Flow API and CompletableFuture
    
*   Plugin System for extensible post types
    
*   JavaFX Desktop UI with data binding
    
*   Spring Boot REST API with WebFlux
    
*   Real-time updates via WebSocket
    
*   Comprehensive testing with JUnit 5 + Mockito
    
Architecture Overview
-----------

`┌─────────────────────────────────────────────────────────────┐
│                     Presentation Layer                      │
│ ┌─────────────────┐ ┌─────────────────────────────────────┐ │
│ │   JavaFX App    │ │       Spring Boot REST API          │ │
│ │   (Desktop UI)  │ │     (Web API + WebSocket)           │ │
│ └─────────────────┘ └─────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────┐
│                       ViewModel Layer                       │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │           FeedViewModel (Reactive Binding)              │ │
│ │         ObservableProperty + Command Pattern            │ │
│ └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────┐
│                        Plugin System                        │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │                   PostPluginManager                     │ │
│ │     Text │ Image │ Video │ Poll │ Link Plugins           │ │
│ └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────┐
│                      Repository Layer                       │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │         FeedRepository (CompletableFuture)              │ │
│ │         + Java Flow API Reactive Streams                │ │
│ └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────┐
│                         Model Layer                         │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │   Immutable Domain Objects (Post, User, etc.)           │ │
│ │       Builder Pattern + Functional Updates              │ │
│ └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘`

Quick Start
-----------

Prerequisites
-------------

*   Java 11+
    
*   Maven 3.6+ or Gradle 7.0+
    

Run Application
---------------

`   # Clone repository
git clone https://github.com/yourusername/java-social-media-feed.git
cd java-social-media-feed

# Run JavaFX Desktop App
mvn clean javafx:run

# Run Spring Boot API
mvn spring-boot:run

# Run tests
mvn test
   `

Project Structure
-----------------

`src/main/java/com/socialmedia/feed/
├── model/                    # Domain models
├── viewmodel/               # MVVM ViewModels  
├── repository/              # Data access layer
├── plugin/                  # Plugin system
├── controller/              # Spring Boot controllers
├── view/                    # JavaFX components
└── util/                    # Utilities
`

Architecture
------------

*   **Model**: Immutable domain objects (Post, User)
    
*   **View**: JavaFX UI components and Spring Boot REST API
    
*   **ViewModel**: FeedViewModel with reactive data binding
    
*   **Plugin System**: Extensible post types (Text, Image, Video, Poll, Link)
    

Key Technologies
----------------

*   Java 11+ with modern language features
    
*   JavaFX 17+ for desktop UI
    
*   Spring Boot 2.7+ for REST API
    
*   Java Flow API for reactive streams
    
*   CompletableFuture for async operations
    
*   JUnit 5 + Mockito for testing
    

API Endpoints
-------------

`GET    /api/feed/posts              # Get posts with pagination
POST   /api/feed/posts/{id}/like    # Like a post
GET    /api/feed/posts/search       # Search posts
WS     /api/feed/stream             # Real-time updates
`

Usage Examples
--------------

Basic Usage
-----------

`FeedRepository repository = new MockFeedRepository();
FeedViewModel viewModel = new FeedViewModel(repository);
PostPluginManager pluginManager = new PostPluginManager();
pluginManager.initializeDefaultPlugins();`


Custom Plugin
-------------

`public class CustomPlugin implements PostPlugin {
    @Override
    public PostType getSupportedPostType() {
        return PostType.QUOTE;
    }
    
    @Override
    public PostViewComponent createView(Post post, FeedViewModel viewModel) {
        return new CustomViewComponent(post, viewModel);
    }
}
`

Testing
-------
`
# Run all tests
mvn test
# Run with coverage
mvn test jacoco:report
# Performance tests
mvn test -Dtest="*PerformanceTest"`



Contributing
------------

1.  Fork the repository
    
2.  Create feature branch (git checkout -b feature/name)
    
3.  Commit changes (git commit -am 'Add feature')
    
4.  Push to branch (git push origin feature/name)
    
5.  Create Pull Request
