# SmartQueue

Thread-Safe, Strongly Typed Smart Queue for Events.  Not meant as a replacement for an EventBus type library, but better
for smaller, more confined tasks. (Though it does scale just fine).

## Features

* Easy to use API
* 5 Levels of Event Prioritization
* Defered Events
* Class based dependencies
* Automatic pruning based on class dependencies and timeout
* More lightweight than other solutions, better for one-off solutions.
* Built in, customizable logging.

### The Record and Builder

SmartQueue has a single access point for adding new events, in ```SmartQueue::createRecord(E,D)```.  This will return 
you a ```RecordBuilder``` instance, which you have the option of either adding more settings for, or simply calling
```RecordBuilder::submit()``` to add the event to the queue.  Through the builder you can set:

* An event type which should occur before this event can occur
* A class dependency for the event, which should be registered via ```SmartQueue::addDependency(Class)```
* A lifetime, in milliseconds.  Defaults to zero which means "infinite"
* A priority, which defaults to ```SmartQueuePriority.Normal```

### The Worker

The worker runs on a single thread, and passes your data through to an instance of ```SmartQueueProcessor``` on the
same thread.  When the worker runs out of things to process, it'll ```wait()``` until more data is available.

### The Processor

In order to instantiate a ```SmartQueue``` you need to implement ```SmartQueueProcessor```.  Make sure you keep a strong
reference of this in your calling class, as ```SmartQueue``` does not hold onto it, it simply passes it to the
```SmartQueueWorker``` constructor, which stores it in a weak reference.  The processor has a single function that takes
E and D, which will be called for each event you add.

### Debug Mode

You can enable debug mode via ```SmartQueue::setDebugEnabled(boolean)```

### Class-based Deference

You might be asking... What if I want to defer a task until a class is registered?  Well, SmartQueue will only defer
based on Event type, that way there's only a single point of deference.  If you want to emulate this, create an event
for your class, and when the class is ready to go, emit the event.

### Validation

Whenever an object is removed, we validate it.  If it has a non-negative TTL, and it's class dependencies are met, we
can go ahead and process it.  The caveat with class dependencies, is that if the event has a defer type attached to it,
we'll still properly defer it.  It won't get lost.  Once the defer event is executed, the event goes back into the queue
and will be revalidated.

### Defer

You can defer events until a certain other event happens via the Builder.  If you do this, whenever that event is
processed, SmartQueue will push it out to a queue just for your defer type.  Once your defer type happens (you push an
event with the type and it gets processed), the defered events will be pushed back into your queue and resume operations.

### Logging

SmartQueue has built in optional logging.  You can extend the ```SmartQueueLogger``` abstract class, and pass it as an
optional parameter to ```SmartQueue.create(/* ... */)```.  This will pipe all internal logging into that abstract class,
and you can decide what to do with it from there.  Five levels of logging are currently supported, but not all are used.
By default, SmartQueue will use an empty implementation.  I've included SystemOutLogger as an available option for
testing purposes, as well as an easy logger to use before you implement your own.