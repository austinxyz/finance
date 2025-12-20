# Claude Code in Practice — Part 4: How AI Rewrites Software Development Best Practices

> Exploring how Agile, TDD, and Code Review evolve in the AI era — from sprints to prompts, from red-green-refactor to contract-driven development.

**Suggested Tags**: Software Engineering, AI Development, Agile, Development Methodologies, Claude Code
**Reading Time**: 12 min read

---

## Evolution of Software Development Methodologies in the AI Era

### Agile Development: From Sprints to Prompts

#### Challenges of Traditional Agile

**Typical Scrum Process**: Sprint Planning → Daily Standup → Development (2 weeks) → Sprint Review → Retrospective

**Problems for Personal Projects**:
- Formal meeting processes too heavy for individual developers
- 2-week Sprint too long, unsuitable for fragmented personal time
- Difficult to maintain strict process discipline

#### AI-Driven "Micro-Iteration" Mode

**Finance Project's Actual Rhythm**:
- **Iteration Cycle**: 1-3 days (not traditional 2 weeks)
- **Iteration Granularity**: Single feature module

**Typical Workflow** (total 5.5 hours):
1. **Requirements Refinement** - Define feature boundaries and user experience (30 minutes)
2. **Architecture Design** - Planning Mode design database and interfaces (30 minutes)
3. **Rapid Iteration Loop** (3.5 hours, multiple iterations):
   - Single development: Implement one small feature (15-20 minutes)
   - Immediate optimization: Testing + UI adjustment + Code Review (15-20 minutes)
   - Repeat cycle: Complete next small feature, optimize again
   - Continuous refactoring: Adjust architecture when issues found
4. **Integration & Release** - PR submission + Deployment + Regression testing + Documentation update (1 hour)

**Key Differences**:
- **No Formal Meetings** - Planning Mode and Git history replace traditional processes
- **Fast Feedback Loop** - Develop today, test tonight
- **More Time Iterating** - 60% time spent refining experience and code quality

#### Shrivu's Insight: "Shoot and Forget"

Quoting Shrivu Shankar's experience:

> "My goal is 'shoot and forget'—set the goal and context, let AI work autonomously, only validate the final PR."

**Application in Finance Project**:

**Inefficient Approach** (step-by-step guidance):
```
You: Create ExpenseBudget entity
Claude: [Generates code]
You: Now add Repository
Claude: [Generates code]
...
```
→ Every step requires human confirmation, inefficient

**Efficient Approach** ("Shoot and Forget"):
```
You: Implement monthly budget settings feature for expense budget (reference expense-requirements.md section 3.1), including:
- Backend: BudgetSettings related Entity, Repository, Service, Controller
- Frontend: BudgetSettingsForm.vue component
- Database: Budget settings table migration script
- Tests: Service layer unit tests

Run tests after completion, fix automatically if tests fail.
Don't ask me every step, make technical decisions yourself.
```
> Note: expense-requirements.md contains multiple feature modules, only implement one small feature each time, not all at once

**Key Learnings**:
- ✅ **Authorize AI to Make Decisions** - Under architecture design guidance, let AI autonomously implement details
- ✅ **Provide Sufficient Context** - Reference docs + constraints
- ✅ **Small Feature Iterations** - Not delivering all features at once, but completing module by module
- ⚠️ **Set Acceptance Criteria** - "Tests pass" + business logic correct

### Test-Driven Development (TDD) New Form

#### Traditional TDD: Red-Green-Refactor

**Classic Process**: Write test (Red) → Write implementation (Green) → Refactor

**Challenges**:
- Writing tests is tedious (especially lots of mocks)
- Tests also need changes during refactoring (double work)

#### AI-Assisted "Contract-Driven Development"

**New Mode Process**:

1. **Define Contract** (API interface design) - Manual
2. **Generate Tests** - AI auto-generates test cases from interface
3. **Implement Code** - AI implements code that passes tests
4. **Human Review** - Check coverage and edge cases

**Advantages**:
- ✅ Tests first, but humans don't write tests
- ✅ AI maintains both tests and implementation (updates synchronously during refactoring, more efficient)
- ✅ Clear contracts, reduced communication costs

**Reality**: Refactoring still requires test changes, but AI can do it quickly:
- Traditional: Manual modify implementation (30 minutes) + Manual adjust tests (30 minutes) = 1 hour
- AI-assisted: AI refactor implementation + Sync update tests = 10 minutes

**Real Case**: When implementing `ExchangeRateService.batchConvert()`, Claude auto-generated 8 test cases (normal conversion, empty input, edge cases, exception handling, etc.), found and fixed null handling issue, all tests passed. Total time 20 minutes (traditional TDD needs 1-2 hours).

### Object-Oriented Design: Can AI Understand Design Patterns?

#### Application of Design Patterns

**Practice Proves**:
- ✅ AI can correctly apply common design patterns (Strategy, Factory, Observer, etc.)
- ✅ AI can identify obvious anti-patterns (like God Class, over-coupling)
- ✅ When explicitly specifying use of certain pattern in architecture design, AI can implement well

**Recommended Practice**:
- In Planning Mode architecture design, clearly state design patterns to use
- Provide clear interface definitions and responsibility division
- AI will correctly apply design patterns and follow SOLID principles

**Case**: Exchange rate conversion strategy pattern - In architecture design specified use of strategy pattern to support multiple exchange rate sources (fixed rate, API retrieval, manual input), Claude correctly implemented strategy interface, multiple strategy implementation classes, and priority-based strategy selection logic, also proactively used `Optional` and Spring dependency injection, conforming to Java best practices.

### Code Review: New Balance of Human-AI Collaboration

#### Pain Points of Traditional Code Review

**Typical Scenario**: Submit PR → Wait for Reviewer (1-2 days) → Receive feedback → Modify → Wait again...

**Personal Project Dilemma**:
- No Reviewer (self-reviewing hard to find issues)
- Easy to introduce bugs and technical debt

#### AI as First-Round Reviewer

**Workflow**:

1. **Claude generates code + self-review** - Check code standards, performance, security, test coverage
2. **Claude submits improvement suggestions** - Auto-fix found issues
3. **Human final review** - Quick scan business logic, check if AI fixes reasonable

**Effect Comparison**:

| Phase | Traditional | AI-Assisted |
|-------|------------|-------------|
| First-round review | Human (1-2 days) | AI (5 minutes) |
| Common issue detection rate | 70% | 90% |
| Fix time | Human (1-2 hours) | AI (10 minutes) |
| Final quality | Good | Good |

**Key Insight**:
> **AI excels at finding technical issues (performance, security, testing), humans excel at finding design issues (maintainability, extensibility, business understanding)**

### Build & Deploy: Simplified Automation Processes

#### Complexity of Traditional CI/CD

**Personal Project Pain Points**:
- Need to remember multiple commands and parameters (Docker build, Git commit, database migration, etc.)
- Too many config files, high maintenance cost
- Easy to forget a step (like forgetting to update deployment after pushing image)

#### Finance Project Automation Practices

**Core Philosophy**: Encapsulate complex processes as simple Skills

**Common Skills**:
- `/docker-build-push` - Auto-build multi-arch images (amd64/arm64) and push to Docker Hub
- `/git-commit-push` - Auto stage, commit (AI generates message), push
- `/mysql-exec` - Auto-load database credentials and execute SQL
- `/setup-java` - Configure Java environment and load database credentials

**Efficiency Comparison**:

| Task | Traditional | Using Skills |
|------|-------------|-------------|
| Build+push image | 5 minutes (multiple commands) | 30 seconds (`/docker-build-push`) |
| Commit code | 2 minutes (3-4 commands) | 10 seconds (`/git-commit-push`) |
| Database migration | 1 minute (look up credentials+execute) | 5 seconds (`/mysql-exec`) |

**Core Value**:
- ✅ **Reduce Cognitive Load** - Don't need to remember complex commands
- ✅ **Reduce Errors** - Auto-handle credentials and environment config
- ✅ **Improve Efficiency** - Multi-step operations become one-click execution

**Key Insight**:
> **Good automation isn't writing more scripts, but making common operations so simple they're "thoughtless"**

---

## Read the Full Series

- [Part 1: Introduction to Vibe Coding](MEDIUM_URL_PART_1)
- [Part 2: Claude Code Overview and Comparison](MEDIUM_URL_PART_2)
- [Part 3: Full Development Cycle Walkthrough](MEDIUM_URL_PART_3)
- [Part 4: AI-Era Software Methodologies](MEDIUM_URL_PART_4) (You are here)
- [Part 5: Use Cases and Limitations](MEDIUM_URL_PART_5)
- [Part 6: Lessons from 41k Lines of Code](MEDIUM_URL_PART_6)

---

**About the Author**: Austin Xu is a software engineer based in the Bay Area, specializing in cloud infrastructure and Kubernetes-based private cloud platforms. Outside of work, he's an avid tennis and pickleball player who has competed in numerous tournaments, including winning a USTA national championship with his team. Austin is passionate about AI-assisted development and actively organizes community events in the Bay Area focused on AI, personal finance, and leadership. This series documents his real-world experience building a 41,000-line family finance application in just 20 hours using Claude Code.

**Project**: https://github.com/austinxyz/finance
**Connect**: https://medium.com/@austin.xyz
