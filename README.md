# Inventive Alerts Solution

This is a solution for an interview homework. The requirement is, briefly, to create an
application to allow one user (a customer) to receive alerts from another customer based
on custom logic they specify.

The scheduling mechanism and logic for specifying the alerts is up to the interviewee.

Here I provide some details on the solution I came up with.

## Running

You need to add a credentials file in `~/inventive-app-credentials.json` that looks like this:

```json
{
  "openAi": {
    "apiKey": "OPENAI_API_KEY"
  },
  "looker": {
    "baseUrl": "SOME_LOOKER_YOU_HAVE_ACCESS_TO",
    "clientId": "YOUR_CLIENT_ID",
    "clientSecret": "YOUR_CLIENT_SECRET"
  }
}
```

Then do the usual:

```
./gradlew run
```

The request and response format is json-based and is self-described in the route files.

## Overall Design

The design is similar to the server-side portion of many web applications. For convenience
I have used an embedded H2 database, but clearly this should be swapped in production for a
real database.

The application uses Kotlin Ktor for managing routes and Exposed for ORM. This was my first
time using both frameworks and I would highly recommend them. I feel like I could be fully
productive with them after only about a day of coding time invested.

There are several files called `*Routes.kt` and `*Schema.kt`. These manage the application
model and corresponding routes. The route files all contain their own route-and-method-specific
DTO classes.

The application also includes a scratch-built scheduler. This will be discussed in more detail
in the next section.

## Entities

- **Customers**: are the users of the application. Customers are the root entity for
  all of the other entities in the application.
- **Data Sources**: are a view into a customer's data. They are a way for a customer to publish
  data and make it available to other customers.
- **Rules**: are a way for a customer to specify a condition when they want something to happen
  based on the contents of a data source.
- **Triggers**: are a way for a customer to specify an action to take when a rule is matched,
  and how often to look for matches.
- **RuleState**: is an internal-only type and is not exposed via the an API route. This allows
  the application to keep track of the state of a rule so that triggers can fire on a change.

## Scheduler Design

This is split across two files, `JobScheduler.kt` and `RuleStateEngine.kt`. The former performs
the logic of managing an event loop and looking for new jobs. The latter compares current data
with historic data to determine if an action should be taken.

The scheduler is a basic event loop. It holds all its work in memory and scans periodically for
new work from the database. This is a very simple design that could be used for an early release
or prototype, yet can easily be modified or replaced with a more sophisticated off-the-shelf tool.

The scheduler uses Quartz style cron expressions for scheduling so one could imagine, for example, switching
to Quartz with relative ease. If there was a desire to continue using a custom scheduling component,
this basic design is amenable to scaling via hash sharding. Another more sophisticated alternative
would be to separate the scheduling component from task execution using a queue or batch processing
framework.

## Open AI Email "Drafting" Feature

The application includes a single trigger that takes a change and drafts an e-mail. One could
imagine this feature actually sending an e-mail at some point, however I didn't have time to
implement this. A sample draft e-mail is shown below:

Prompt:

> You are an assistant to a marketing executive that works for a high-end clothing retailer. Every day you
> receive a list of products that have either changed in price above $200, are new and are above $200, or
> have dropped below $200.
>
> The raw data is provided json-formatted. A null value for the oldValues field indicates that the product has
> no prior history.
>
> The raw data for the price changes are as follows:
>
> {"kind":"NEW","customer":"test datasource","rule":"test rule","currentValues":{"order_items.sale_price":"
> 293.3299865722656","products.category":"Pants","products.item_name":"True Religion Men's Ricky Straight Corduroy
> Pant"},"oldValues":null}
>
>
> {"kind":"CHANGED","customer":"test datasource","rule":"test rule","currentValues":{"order_items.sale_price":"219","
> products.category":"Pants","products.item_name":"Eddie Bauer First Ascent Heyburn 2.0 Pants"},"oldValues":{"
> order_items.sale_price":"223","products.category":"Pants","products.item_name":"Eddie Bauer First Ascent Heyburn 2.0
> Pants"}}
>
> Please summarize this information in a way that is easy for the marketing executive to understand. Make sure
> that all the price change information is clearly reflected in bulleted form. Include a section for helpful
> marketing ideas at the end.

Response:

> Price Changes:
> - New product "True Religion Men's Ricky Straight Corduroy Pant" with a sale price of $293.33
> - Price change for "Eddie Bauer First Ascent Heyburn 2.0 Pants" from $223 to $219
>
> Helpful Marketing Ideas:
> - Highlight the new True Religion Men's Ricky Straight Corduroy Pant as a high-end addition to the inventory
> - Use the price drop for the Eddie Bauer First Ascent Heyburn 2.0 Pants as a promotion or sale opportunity

## Improvements

There are several areas I would highlight for improvement. First, the test coverage is low and there are no
doubt bugs in the application owing to the low test coverage. I have also mentioned an interest in an alternative
scheduling mechanism.

The exception handling could be improved; the scheduler will currently crash and swallow any exceptions that happen
during execution and the routes could provide better handling especially for 404's and other basic errors with user
requests. I would also generally advise adding some backpressure features and scale testing such an app.

I would also like to have added some more monitoring capabilities via Prometheus and a sample deployment using Docker.

It would be very cool to add an experimentation and eval framework to allow customers to see how their generated content
(emails or other) perform over time, and would also provide an operator the opportunity to measure drift occuring
in the model responses. Such drift could itself become an alert.
