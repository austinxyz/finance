# Chapter 1: Introduction - A New Paradigm for Programming in the AI Era

[← Back to Contents](./00-contents.md) | [Next Chapter: Claude Code Overview →](./02-claude-code-overview.md)

---

## 1.1 From Copilot to Vibe Coding

During 2021-2023, AI-assisted programming was primarily **"completion-based"** tools: GitHub Copilot provided next-line suggestions as you typed code. While revolutionary at the time, this was still the traditional "human writes code, AI assists" model.

In 2024, GitHub Copilot matured and gained widespread adoption. In the first half of 2025, with the emergence of tools like Claude Code, Cursor, and Gemini CLI, a new programming paradigm gradually took shape. By the second half of 2025, the "Vibe Coding" model matured, fundamentally changing the programming paradigm:

- **Traditional Programming**: Developer → Write code → Compile/Test → Debug
- **Vibe Coding**: Developer → Describe requirements → AI generates code → Human review

As Martin Fowler said in his late 2025 interview:
> "AI won't replace programmers, but it will change the nature of programming. Future programmers will be more like 'architects + reviewers' rather than 'code craftsmen'."

---

## 1.2 What is Vibe Coding

"Vibe Coding" is the community's colorful description of a new generation of AI-assisted programming. The core concept is:

> **"Shoot and Forget"** - Like launching a missile, set the target and context, let AI autonomously complete the task, humans only verify the final PR.

**Three Key Characteristics**:
1. **High-level Intent Expression** - Use natural language to describe "what to do" rather than "how to do it"
2. **Autonomous Task Execution** - AI plans steps, writes code, runs tests, submits code
3. **Result-focused Validation** - Humans focus on code review and final results, not the process

This model is fundamentally different from traditional "code completion":
- **Copilot Mode**: Humans write code, AI provides next-line suggestions (assistance)
- **Vibe Coding**: Humans provide requirements, AI completes the entire development workflow (leadership)

**Term Origin**:
The term "Vibe Coding" originated from the developer community's playful description of the new AI programming approach, first appearing in 2024 on Twitter/X and Reddit developer discussions. It vividly describes the new paradigm of "completing programming by conveying intent (vibe) rather than writing code."

**Advocates**:
- **Anthropic**: Provides complete CLI autonomy through Claude Code, the main driver of Vibe Coding
- **Cursor**: Deeply integrates AI into IDE, lowering the barrier to Vibe Coding
- **Replit**: AI Agent mode, users describe requirements to generate complete applications

**Current Status** (2025):
- Has moved from "tech preview" to "production ready" stage
- Suitable for personal projects and small-to-medium teams, large enterprises still in pilot phase
- Mainstream tools: Claude Code, Cursor, Cline, Gemini CLI
- Active community, many best practices and workflows forming

---

## 1.3 The Value of Vibe Coding

Why can Vibe Coding change software development efficiency?

**1. Eliminates Repetitive Labor**
- Traditional: CRUD interfaces require manually writing Controller, Service, Repository, Entity, DTO...
- Vibe Coding: Describe requirements in one sentence, AI generates all files with consistent naming

**2. Reduces Context Switching Costs**
- Traditional: Backend → Frontend → Database → Deployment, switching between multiple tech stacks
- Vibe Coding: AI masters full-stack technology, completes cross-layer changes in one go

**3. Enables "Think-speed Programming"**
- Traditional: From idea to code takes hours or even days
- Vibe Coding: From idea to runnable code takes only minutes

**Real Case** (Finance Project):
- Adding expense budget feature (backend API + frontend page + database script)
  - Traditional approach: ~4-6 hours
  - Vibe Coding: 45 minutes (including testing and fixes)
  - **Efficiency improvement: 5-8x**

**4. Focus on "What" Rather Than "How"**

In the Finance project, my focus shifted from "writing code" to:
- **Requirement iteration and UI refinement** (35%) - New ideas naturally emerge after seeing the interface, rapid iteration
- **Architecture refactoring and pattern application** (30%) - While adjusting one interface, refactor similar features
- **Learning best practices and workflow optimization** (20%) - Explore best workflows with Claude Code, reduce wait times
- **Code review and quality control** (15%) - Ensure generated code meets standards

**Real Experience**:
- Early on, I took many detours, wasting time waiting for AI generation and debugging
- As I became familiar with the tools (Skills, Hooks, CLAUDE.md), efficiency improved significantly
- The iteration speed of "see interface → generate idea → immediately implement" is incomparable to traditional development

This is what Martin Fowler meant by "programmers more like architects + reviewers," but the actual work is more flexible—a hybrid role of **architect + product manager + reviewer**.

---

## 1.4 The Value of This Article

While there are many introductions to AI programming tools, this article's unique aspects are:

- ✅ **Real Project Validation** - All practices come from the Finance project (GitHub: [austinxyz/finance](https://github.com/austinxyz/finance))
- ✅ **100% AI Generated** - Approximately 41,000 lines of code entirely generated by Claude Code, true Vibe Coding practice
- ✅ **Full Lifecycle Coverage** - Complete software engineering lifecycle from requirements analysis to production deployment
- ✅ **Concrete and Actionable** - Provides actual configuration files, slash commands, skills code examples
- ✅ **Theory Meets Practice** - References Martin Fowler interviews and Shrivu Shankar's in-depth experience
- ✅ **Data-driven** - Net development time approximately 20 hours, all data verifiable

**Finance Project Overview**:
- **Tech Stack**: Spring Boot 3.2 + Java 17 + Vue 3 + MySQL 8.0 + Docker
- **Feature Scope**: Asset management, liability tracking, expense analysis, investment management, multi-currency support, data visualization
- **Code Scale**: 127 Java files, 33 Vue components, 25 database tables, approximately 41,000 lines of code
- **Development Mode**: Completely using Claude Code, building efficient workflows through Skills, Hooks, CLAUDE.md

**Target Audience**:
- Individual developers wanting to try Vibe Coding
- Technical managers considering introducing AI tools to teams
- Practitioners interested in software engineering methodology evolution
- Full-stack engineers looking to improve development efficiency

---

[← Back to Contents](./00-contents.md) | [Next Chapter: Claude Code Overview →](./02-claude-code-overview.md)
