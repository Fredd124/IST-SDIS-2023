# DistLedger

Distributed Systems Project 2022/2023

## Authors

**Group A29**

### Introduction

For the 3rd phase we implemented the gossip architecture within our project, in order to achieve availability while keeping causal ordering, which garantees some consistency.

### Changes from 2nd to 3rd phase

On the 2nd phase of the project we added a primary backup replication system, which had the primary server propagating after every update. So the first change was to remove this constraint and change the propagation code to the admin server implementation, because now it's the admin that invokes the gossip command, which triggers propagation.

### Decisions made

- We considered that a server not being active represents a fault for that server, so no gossip request is sent or answered when a server is inactive.

- We chose to make a client wait on a query when its clock isn't smaller or equal to the valueTS. We made this decision based on our agreement that a client expects a response after a query, and it's better to wait for that response than to just get an exception, knowing the query didn't work. The replica manager will then respond to the client, as soon as its valueTS catches up to the client's clock.

- In order to reduce the number of operations sent in every gossip request, we implemented a timestamp table for every replica manager. Upon a gossip request, the replica manager will access its timestamp table for each replica, and filter the operations that have its operation.TS smaller than or equal to the timestamp table entry, for that replica. When the propagate request is sent, each replica manager will update its timestamp table with the replicaTS received from the request.

- We considered that when an updated can be executed and it's not because it throws an exception, that operation is made stable to prevent it form trying to execute every time. This way we save processing time and prevents execution of a previous operation due to update of value (e.g alice doesn't have enough money but after a gossip she gets a transfer from broker and can now execute the previous operation).
