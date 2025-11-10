A real-time distributed chat application built on Android that demonstrates core distributed systems principles in action. The app leverages multiple cloud services working in concert to deliver seamless messaging capabilities.

**Architecture Overview:**
Firebase Authentication - Distributed identity management
Firestore/Firebase Realtime Database - Multi-region data synchronization
Cloudinary CDN - Global media distribution network
Android Client - Edge computing nodes

**Distributed Features:**
Multi-user concurrent sessions with eventual consistency
Geo-distributed message replication across Firebase regions
Content delivery network for optimized media streaming
Decentralized presence management
Fault-tolerant image uploads with redundant cloud storage

The system maintains ACID properties for user data while providing BASE semantics for real-time message propagation, showcasing practical CAP theorem implementation in a production mobile environment
