# Trading Engine System

## Overview

The Trading Engine System is a sophisticated trading platform designed to handle and process buy and sell orders for financial instruments. It supports real-time order execution and market data management, utilizing advanced concurrency models to ensure efficient and reliable trading operations.

## Features

- **Real-Time Order Processing**: Handles buy and sell orders with low latency and high throughput.
- **Concurrency Management**: Combines traditional multi-threading with Akka actor-based concurrency for scalable and thread-safe operations.
- **Market Data Management**: Provides up-to-date market prices and trading volume records.
- **Order Tracking**: Allows users to retrieve the status of their orders, including completed and pending orders.

## Technologies Used

- **Languages**:
  - **Java**: Core programming language used for implementing the trading engine and web server.

- **Toolkit and Libraries**:
  - **Akka**: Used for actor-based concurrency, enhancing scalability and handling concurrent tasks efficiently.
  - **Log4j**: Utilized for logging system events, errors, and debug information to aid in troubleshooting and monitoring.
  - **Servlet API**: Provides HTTP request and response handling capabilities, used to interact with the trading engine through a web interface.

## Project Structure

- **`TradingEngineServlet.java`**: A servlet-based implementation for handling HTTP requests related to buying, selling, and querying order results.
- **`TradingEngineServletWithActors.java`**: An updated servlet implementation using Akka actors for improved concurrency and performance.
- **`TradingEngineThread.java`**: Implements a multi-threaded approach to manage trading operations, providing methods for adding orders and retrieving market data.
- **`Constants.java`**: Contains configuration constants used across the trading engine, such as delay times, product IDs, and timeout values.
- **`TradingEngine.java`**: Core trading engine class responsible for managing and processing trading orders and market data.

## Setup and Installation

1. **Clone the Repository**

   ```bash
   git clone https://github.com/aldebaran2309/trading-engine.git
   cd trading-engine



Build the Project

Ensure that Apache Maven is installed on your system. Run the following command to compile and package the project:

mvn clean install


Deploy the Application

Deploy the compiled WAR file to a servlet container such as Apache Tomcat. You can also run the application directly from your Java IDE.

Configure Akka

Ensure that the Akka configuration files are properly set up for actor-based concurrency. Adjust the configuration as needed in the application.conf file (if applicable).

Usage
Place a Buy Order:

Endpoint: GET /buy2?productId={productId}&quantity={quantity}&userId={userId}
Description: Places a buy order for the specified product and quantity.
Place a Sell Order:

Endpoint: GET /sell2?productId={productId}&quantity={quantity}&price={price}&userId={userId}
Description: Places a sell order for the specified product, quantity, and price.
Retrieve Order Result:

Endpoint: GET /result2?id={orderId}
Description: Fetches the result of the order identified by orderId. Returns the status or result of the order.
Code Overview
Servlet-Based Implementation:

TradingEngineServlet.java: Handles HTTP requests and interacts with the trading engine. Processes buy and sell requests and provides results through HTTP responses.
Actor-Based Implementation:

TradingEngineServletWithActors.java: Utilizes Akka actors for managing trading operations, improving concurrency and scalability. This version supports asynchronous message handling and actor-based processing.
Multi-Threaded Engine:

TradingEngineThread.java: Provides a multi-threaded approach to order processing, ensuring thread safety and efficient operation.
