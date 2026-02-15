-- Insert sample QuestionMain: Design Twitter
INSERT INTO question_main (name, description, write_up, created_at, updated_at)
VALUES (
    'Design Twitter',
    'Design a social media platform similar to Twitter that allows users to post short messages (tweets), follow other users, and view a timeline of tweets from people they follow.',
    '# Sample Answer: Design Twitter

## 1. Functional Requirements
- Users can post tweets (max 280 characters)
- Users can follow/unfollow other users
- Users can view a personalized timeline
- Users can like and retweet posts
- Users can search for tweets and users

## 2. Non-Functional Requirements
- High availability (99.9% uptime)
- Low latency for timeline generation (<500ms)
- Scalable to 500M daily active users
- Handle 10K tweets per second

## 3. Core Entities
- User (user_id, username, email, created_at)
- Tweet (tweet_id, user_id, content, timestamp, like_count, retweet_count)
- Follow (follower_id, followee_id, created_at)

## 4. API Design
- POST /api/v1/tweets - Create a new tweet
- GET /api/v1/timeline/{user_id} - Get user timeline
- POST /api/v1/users/{user_id}/follow - Follow a user
- GET /api/v1/tweets/search?q={query} - Search tweets

## 5. High-Level Design
### Architecture:
- Load Balancer (distributes traffic)
- API Gateway (authentication, rate limiting)
- Application Servers (stateless, horizontally scalable)
- Tweet Service (handles tweet CRUD operations)
- Timeline Service (generates user timelines)
- Follow Service (manages user relationships)
- Cache Layer (Redis - timeline cache, hot data)
- Database (PostgreSQL - user data, MySQL sharded - tweets)
- Message Queue (Kafka - async processing)
- CDN (static assets)

### Timeline Generation (Fan-out on write):
1. User posts tweet
2. Write to Tweet DB
3. Fanout service reads from Kafka
4. Push to Redis cache for each follower
5. Users read timeline from Redis (fast)

## 6. Deep Dive: Timeline Generation at Scale
### Challenge: Celebrity users with millions of followers

**Solution: Hybrid approach**
- Regular users: Fan-out on write (push to Redis)
- Celebrity users: Fan-out on read (pull from DB on demand)
- Threshold: 1M followers

### Timeline Cache Structure:
```
Key: timeline:{user_id}
Value: Sorted Set (tweet_id, timestamp)
TTL: 24 hours
Max entries: 1000 tweets
```

### Read path optimization:
1. Check Redis cache
2. If miss, query Tweet DB with user''s follow list
3. Merge tweets, sort by timestamp
4. Cache result
5. Return top 50 tweets',
    NOW(),
    NOW()
);

-- Get the ID of the inserted QuestionMain
-- Note: Using specific ID (1) since this is seed data and table should be empty

-- Insert Questions for "Design Twitter"
INSERT INTO question (question_main_id, "order", type, name, description, whiteboard_section, requires_recording, created_at)
VALUES
    -- Question 1: Functional Requirements
    (
        1,
        1,
        'FUNCTIONAL_REQ',
        'Define Functional Requirements',
        'What are the core features that Twitter must support? Think about user actions like posting, following, and viewing content. List 4-6 key functional requirements.',
        1,
        false,
        NOW()
    ),
    -- Question 2: Non-Functional Requirements
    (
        1,
        2,
        'NON_FUNCTIONAL_REQ',
        'Define Non-Functional Requirements',
        'What are the system quality attributes? Consider scalability (number of users), performance (latency requirements), availability (uptime), and consistency requirements.',
        2,
        false,
        NOW()
    ),
    -- Question 3: Entities
    (
        1,
        3,
        'ENTITIES',
        'Identify Core Entities',
        'What are the main data models in the system? Define entities like User, Tweet, Follow, etc. Include key attributes for each entity (don''t worry about all fields, just the essential ones).',
        3,
        false,
        NOW()
    ),
    -- Question 4: API Design
    (
        1,
        4,
        'API',
        'Design Key APIs',
        'What are the main API endpoints needed for Twitter? Design 4-5 critical APIs with HTTP methods, paths, request/response formats. Examples: POST /tweets, GET /timeline/{user_id}, etc.',
        4,
        false,
        NOW()
    ),
    -- Question 5: High-Level Design
    (
        1,
        5,
        'HIGH_LEVEL_DESIGN',
        'Create High-Level System Architecture',
        'Draw the overall system architecture. Include: load balancers, API gateway, application servers, databases, caches, message queues, and CDN. Show how data flows from client to backend services. Explain your design choices.',
        5,
        true,
        NOW()
    ),
    -- Question 6: Deep Dive
    (
        1,
        6,
        'DEEP_DIVE',
        'Deep Dive: Timeline Generation at Scale',
        'How do you generate a user''s timeline efficiently when they have millions of followers? Compare fan-out on write vs fan-out on read. How would you handle celebrity users with millions of followers? What caching strategy would you use?',
        5,
        true,
        NOW()
    );

-- Insert sample QuestionMain: Design URL Shortener
INSERT INTO question_main (name, description, write_up, created_at, updated_at)
VALUES (
    'Design URL Shortener',
    'Design a URL shortening service like bit.ly or TinyURL that converts long URLs into short, unique aliases that redirect to the original URL.',
    '# Sample Answer: Design URL Shortener

## 1. Functional Requirements
- Given a long URL, generate a short unique alias
- Redirect short URL to original long URL
- Optional: Custom aliases
- Optional: Analytics (click tracking)
- Optional: Expiration time for URLs

## 2. Non-Functional Requirements
- High availability (99.99% uptime - critical for links in production)
- Low latency for redirects (<100ms)
- Scalable to 1 billion URLs
- Handle 10K URL creations per day, 100K redirects per day

## 3. Core Entities
- URL Mapping (short_code, original_url, created_at, expiry_at, user_id)
- Analytics (short_code, click_count, last_accessed_at)

## 4. API Design
- POST /api/v1/shorten - Create short URL
  Request: {"url": "https://example.com/very/long/url"}
  Response: {"short_url": "https://short.ly/abc123"}

- GET /{short_code} - Redirect to original URL
  Response: 302 Redirect

- GET /api/v1/analytics/{short_code} - Get click stats
  Response: {"clicks": 1523, "created_at": "2026-01-01"}

## 5. High-Level Design
### Architecture Components:
- Load Balancer (distributes traffic)
- API Servers (stateless, auto-scaling)
- Short URL Generation Service
- Database (PostgreSQL/MySQL - URL mappings)
- Cache Layer (Redis - hot URLs, 80/20 rule)
- Analytics Service (async processing)

### Short Code Generation:
**Approach: Base62 encoding**
- Characters: [a-z, A-Z, 0-9] = 62 characters
- Length: 7 characters = 62^7 = 3.5 trillion combinations
- Hash function: MD5(original_url + timestamp) → take first 7 chars

### Database Schema:
```sql
CREATE TABLE url_mappings (
    short_code VARCHAR(10) PRIMARY KEY,
    original_url TEXT NOT NULL,
    created_at TIMESTAMP,
    expiry_at TIMESTAMP,
    user_id BIGINT,
    INDEX idx_user_id (user_id)
);
```

### Read Path (Redirection):
1. User clicks short URL
2. Check Redis cache
3. If hit: redirect (99% of requests)
4. If miss: query DB, update cache, redirect

## 6. Deep Dive: Ensuring Uniqueness at Scale
### Challenge: How to generate unique short codes without collisions?

**Option 1: Random Generation with Retry**
- Generate random 7-char code
- Check DB for collision
- Retry if collision (rare)
- Pros: Simple, distributed-friendly
- Cons: Possible retries, not deterministic

**Option 2: Counter-based + Base62**
- Use distributed counter (Redis INCR)
- Convert counter to Base62
- Pros: No collisions, predictable
- Cons: Single point of failure (Redis)

**Option 3: Pre-generated Key Pool**
- Background job generates keys
- Store in key pool table
- API servers fetch from pool
- Pros: Fast, no generation overhead at request time
- Cons: Complex, need monitoring

**Recommended: Option 2 with Redis Cluster for HA**

### Handling Collisions:
```python
def generate_short_code(url, attempt=0):
    hash_input = url + str(timestamp) + str(attempt)
    hash_value = md5(hash_input)
    short_code = base62_encode(hash_value)[:7]
    
    if db.exists(short_code):
        return generate_short_code(url, attempt + 1)
    
    return short_code
```

### Caching Strategy:
- Cache hot URLs (80/20 rule)
- TTL: 24 hours
- Cache key: short_code
- Cache value: original_url
- Cache aside pattern',
    NOW(),
    NOW()
);

-- Insert Questions for "Design URL Shortener"
INSERT INTO question (question_main_id, "order", type, name, description, whiteboard_section, requires_recording, created_at)
VALUES
    -- Question 1: Functional Requirements
    (
        2,
        1,
        'FUNCTIONAL_REQ',
        'Define Functional Requirements',
        'What are the core features of a URL shortener? Consider URL shortening, redirection, custom aliases, analytics, and expiration. List 4-5 key functional requirements.',
        1,
        false,
        NOW()
    ),
    -- Question 2: Non-Functional Requirements
    (
        2,
        2,
        'NON_FUNCTIONAL_REQ',
        'Define Non-Functional Requirements',
        'What are the system quality attributes for a URL shortener? Consider availability (uptime requirements - critical for production links), latency (how fast should redirects be?), scalability (how many URLs?), and data retention.',
        2,
        false,
        NOW()
    ),
    -- Question 3: Entities
    (
        2,
        3,
        'ENTITIES',
        'Identify Core Entities',
        'What are the main data models? Define URL Mapping entity with attributes like short_code, original_url, created_at, etc. Consider if you need separate entities for analytics.',
        3,
        false,
        NOW()
    ),
    -- Question 4: API Design
    (
        2,
        4,
        'API',
        'Design Key APIs',
        'What are the main API endpoints? Design APIs for: (1) creating short URLs, (2) redirecting to original URL, (3) getting analytics. Include HTTP methods, paths, request/response formats.',
        4,
        false,
        NOW()
    ),
    -- Question 5: High-Level Design
    (
        2,
        5,
        'HIGH_LEVEL_DESIGN',
        'Create High-Level System Architecture',
        'Draw the overall system architecture. Include: load balancer, API servers, database, cache layer (Redis for hot URLs), and analytics service. Show the read path (redirection) and write path (URL creation). Explain your caching strategy.',
        5,
        true,
        NOW()
    ),
    -- Question 6: Deep Dive
    (
        2,
        6,
        'DEEP_DIVE',
        'Deep Dive: Short Code Generation and Uniqueness',
        'How do you generate unique short codes at scale? Compare different approaches: (1) random generation with collision checking, (2) counter-based approach, (3) pre-generated key pool. How do you handle collisions? What encoding scheme would you use (Base62, Base64)?',
        5,
        true,
        NOW()
    );
