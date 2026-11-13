# FutureTech

## Overview

University senior project with a hard deadline of about 3-4 months (plan: 15 weeks).
FutureTech is a web-based computer store selling pre-built laptops and desktops,
accessories and peripherals, and PC components (CPU, RAM, storage, etc.).
Its main feature is **ForgeAI**, a PC-building engine.

## About me (the developer)

- Student with experience in Java (OOP, JavaFX), Spring Boot, Angular, TypeScript,
  HTML/CSS/JS, and PostgreSQL (from my internship).
- I must be able to explain and defend all of the code to my advisor, so keep it
  simple, readable, and briefly explained.

## Stack

- Backend: Java 21, Spring Boot, Spring Data JPA, Spring Security (JWT), Maven
- Frontend: Angular + TypeScript, Angular Material
- Database: PostgreSQL
- AI: LLM API (Claude API) called from Spring Boot over HTTP/JSON
- Tests: JUnit 5 (backend), focus on the compatibility rules
- Tools: Git/GitHub, Postman

## Project structure

- `/backend` Spring Boot application
- `/frontend` Angular application

## Features

1. **Store:** product catalog, search/filter, cart, fake checkout (NO real
   payments), order history
2. **Customers:** accounts (customer and admin roles), purchase history, points
   earned automatically per purchase
3. **Warranty tracking:** warranty period and expiry recorded per purchased item
4. **Admin:** manage products, customers, orders
5. **ForgeAI:**
   - AI recommendations: user enters budget, specs, and intended use (gaming,
     editing, office, etc.) and the AI suggests builds
   - Manual builder: user picks parts and gets live compatibility checking
   - Budget optimizer: suggest ways to cut cost while keeping performance, or a
     more balanced build when a small performance cut saves a lot of money
6. **Rewards system:** FUTURE WORK. Do NOT build it unless I ask.

## ForgeAI design rules

- Compatibility checking is **rule-based plain Java, NOT AI**. One class per rule
  (socket, RAM type, PSU wattage, case size, GPU length, cooler height), all
  implementing a common interface. Every rule must have unit tests.
- The LLM may only choose parts from catalog data we send it, and must reply in
  strict JSON.
- Every AI-suggested build is validated by the rule engine before it is shown.
  If validation fails, send the errors back to the LLM once to fix the build.
- Each part has a performance score; price-to-performance is used by the
  budget optimizer.
- Handle LLM failures gracefully (timeouts, bad JSON) with a clear fallback.

## Conventions

- Layers: controller -> service -> repository; DTOs for API responses
- REST endpoints under `/api`
- Clear names, small classes, short comments where logic is not obvious
- Secrets (API keys, DB password) only via environment variables, never committed
- Seed realistic demo data (about 50-100 products with proper specs)
- Commit to git after every working feature

## Working rules for Claude

- Work in small steps, one feature at a time. After each step, tell me how to run
  and test it.
- Propose a plan and wait for my approval before large changes or new files in bulk.
- Briefly explain what you built and why, so I can learn from it.
- Keep it simple and finish-able; this is a 3-4 month project. No unnecessary
  frameworks, Docker, or cloud setup unless I ask.
- Ask me if a requirement is unclear instead of guessing.

## Roadmap (about 15 weeks)

1. Week 1: requirements, database design (ERD), project setup
2. Weeks 2-3: entities, repositories, product catalog API, seed data
3. Weeks 4-5: Angular storefront (list, search/filter, product page, cart)
4. Week 6: authentication, fake checkout, orders
5. Weeks 7-8: customer profile, points, warranty tracking, admin pages
6. Weeks 9-10: ForgeAI part 1 (compatibility rule engine, manual builder)
7. Weeks 11-12: ForgeAI part 2 (LLM recommendations, budget optimizer)
8. Week 13: testing and bug fixing
9. Week 14: UI polish and demo data
10. Week 15: final report, slides, demo rehearsal

## Current status

(Update this section after each session.)

- Phase: 1 (not started)
- Done: nothing yet
- Next: database schema and backend project setup
