# Claude Code in Practice — Part 5: When to Use (and Not Use) Claude Code

> Honest analysis of Claude Code's strengths, weaknesses, and the costly mistakes that taught me valuable lessons.

**Suggested Tags**: Claude Code, AI Development, Best Practices, Software Engineering, Developer Productivity
**Reading Time**: 13 min read

---

## Use Cases and Limitations of Claude Code

### Best Use Cases

Based on Finance project experience, Claude Code excels in these scenarios:

#### 1. Rapid Prototyping of New Projects

**Typical Scenarios**:
- Converting existing workflows (like Excel/Spreadsheet) to Web applications
- Quickly validating product idea feasibility
- Exploring new development modes (like Vibe Coding)

**Finance Project Validation**:
- **Background**: Migrate Spreadsheet financial management to Web app
- **Tech Stack**: Reuse existing project tech stack (Spring Boot + Vue 3)
- **Development Mode**: First try Vibe Coding (completely new development experience)
- Zero to runnable MVP: **2 days** (traditional approach needs 1-2 weeks)
- Basic feature completeness: 85%
- Code quality: Can directly enter iteration phase

**Key Success Factors**:
- Tech stack is mainstream (Spring Boot, Vue 3)
- Clear requirements (based on existing Spreadsheet workflow)
- Vibe Coding mode significantly improves development efficiency

#### 2. Feature Extension of Existing Projects

**Typical Scenarios**:
- Adding reporting module to existing CRM system
- Adding comment feature to blog system
- Extending API to support new data formats

**Finance Project Validation**:

Adding "expense budget management" feature:
- Involves: 2 new tables, 4 APIs, 3 frontend pages
- Traditional estimate: 3-5 days
- Actual time: **8 hours** (Claude Code assisted)
- Code reuse rate: 70% (referenced existing expense module)

**Claude Code Advantages**:
- ✅ Understands existing code style (via CLAUDE.md)
- ✅ Reuses existing components (Service layer, DTO pattern)
- ✅ Maintains consistency (naming, comments, tests)

#### 3. Systematic Refactoring

**Typical Scenarios**:
- Applying new UI design guidelines to existing pages
- Cross-module feature enhancement (like multi-currency support)
- Unified UX optimization (like mobile adaptation)

**Finance Project Validation**:

**Task 1**: Refactor asset & liability analysis page with expense analysis UI design guidelines
- Involves: Style and layout refactoring of 6 Vue components
- Traditional: 2-3 days (need repeated comparison and adjustment)
- Claude Code: **6 hours**
- Style consistency: 95%+

**Task 2**: System-wide multi-currency support
- Involves: Database migration, backend Service layer refactoring, frontend component updates
- Traditional: 1 week (cross-layer changes, easy to miss)
- Claude Code: **2 days**
- AI auto-identifies all locations needing modification

**Task 3**: Optimize all UI for mobile experience
- Involves: Responsive layout adjustment for 33 Vue components
- Traditional: 3-4 days
- Claude Code: **1 day**
- Uniformly apply Tailwind responsive classes

#### 4. Documentation and Test Writing

**Typical Scenarios**:
- Writing unit tests for complex business logic
- Generating and syncing project documentation
- Updating design docs and database docs after iterations

**Finance Project Validation**:

| Task | Traditional Time | AI Time | Quality Assessment |
|------|-----------------|---------|-------------------|
| Unit tests (30 Services) | 8 hours | 1.5 hours | 85%+ coverage |
| Requirements doc sync (post-iteration) | 3 hours | 30 minutes | Needs manual review |
| Design doc update | 2 hours | 20 minutes | Accurately reflects changes |
| Database doc sync | 2 hours | 15 minutes | Complete and accurate |
| Architecture diagrams (Mermaid) | 1 hour | 5 minutes | Clear and accurate |

**Greatest Value**:
- **Test Writing**: AI-generated test cases cover more edge cases
- **Doc Sync**: After multiple iterations, doc differences large, AI can quickly align code and docs
- **Efficiency Boost**: Doc maintenance from "most hated task" to "done in 10 minutes"

### Unsuitable or Need Caution Scenarios

#### 1. Complex Business Logic Implementation

**Case**: Annual financial summary algorithm (Finance actual case)

**Requirements**: Complex aggregation calculations across accounts, currencies, time (implemented using stored procedure)

**Implementation Process** (multiple iterations):
1. **Version 1**: AI generates basic aggregation logic
   - ❌ Didn't consider multi-currency conversion
   - ❌ Poor performance (15s response time)

2. **Version 2**: Discussed optimization with AI
   - ✅ Added currency conversion logic
   - ⚠️ Still has performance issues

3. **Version 3**: Human intervention in design
   - ✅ Refactored to batch operations
   - ✅ Optimized to 0.8 seconds

**Conclusion**:
- ⚠️ AI can implement complex logic, but needs multiple iterations and human guidance
- ❌ Initial version often misses key edge cases (like multi-currency, performance optimization)
- 🎯 **Strategy**: Step-by-step implementation + continuous discussion + human review of critical logic

#### 2. Performance-Critical Low-Level Code

**Case**: Large-volume financial report generation

**Requirements**: Generate trend report with 50+ accounts × 12 months × 5 years = 3000 records, response time requirement <1 second

**Claude Version 1** (inadequate):
- Loop database queries (300 queries)
- Response time: 15 seconds

**Human Optimization** (using stored procedure):
- One-time aggregation of all data
- Response time: 0.8 seconds
- **Performance improvement: 18x**

**Conclusion**:
- ❌ AI tends toward "works is enough" implementation (ignores performance)
- ⚠️ Performance-critical paths need human design and profiling
- ✅ AI can generate stored procedure code (given clear requirements)

### Applicability in Team Collaboration Scenarios

While Finance is a personal project, based on Shrivu's enterprise experience and Claude Code characteristics, we can infer applicability to team scenarios:

#### Suitable Team Scenarios

**1. Small Teams (2-5 people)**
- ✅ Unified CLAUDE.md can serve as "code constitution"
- ✅ Skills can encapsulate team toolchain (deployment, testing, code checking)
- ✅ Hooks can enforce code standards (like pre-commit testing)

**Example**: A 3-person startup team's practice
- Maintain a 13KB CLAUDE.md (tech stack + coding standards)
- 5 shared Skills (deployment, database, API testing, doc generation, code formatting)
- 2 Hooks (tests must pass to commit, sensitive data check)

**Results**:
- New member onboarding: From 2 weeks → 3 days
- Code style consistency: 95%+
- Repetitive work reduction: 60%

**2. Open Source Project Contribution**
- ✅ CLAUDE.md can serve as contributor guide
- ✅ AI helps new contributors understand codebase
- ✅ AI generates PRs conforming to project standards

**Recommended Practice**: In CLAUDE.md explain development workflow, common pitfalls, code standards, helping contributors get started quickly

#### Unsuitable Team Scenarios

**1. Large Enterprises (100+ engineers)**
- ❌ CLAUDE.md hard to cover all teams' special needs
- ❌ Complex code review process (needs multi-level approval)
- ⚠️ Security and compliance issues (AI accessing sensitive code)

**Solution** (Shrivu's enterprise practice):
- Each product line maintains its own CLAUDE.md (inheriting company-level standards)
- Use Claude Code GHA (GitHub Actions) rather than local CLI (better auditing)
- Limit AI access scope (can only access specific codebases)

**2. Highly Regulated Industries (Finance, Healthcare)**
- ❌ AI-generated code needs strict compliance review
- ❌ Code ownership and liability issues (who's responsible for AI-written bugs?)
- ⚠️ Data privacy issues (code may contain sensitive info)

**Recommendations**:
- Only use AI in non-core, non-sensitive modules
- All AI-generated code must have human review + legal/compliance approval
- Use privately deployed models (like Claude for Enterprise)

### Common Mistakes & Efficiency Traps

During Finance project development, I made some mistakes that significantly reduced Claude Code efficiency. These lessons worth sharing:

#### Mistake 1: Key Constraints Not Codified in CLAUDE.md or Skills

**Problem Manifestation**:
- Claude keeps making same mistakes (like using wrong database connection method)
- Every session must repeatedly emphasize same rules
- Waste lots of tokens correcting errors

**Real Cases**:
- First 3 sessions, Claude always used `mvn` instead of `./mvnw`
- Repeatedly forgot to use `TimeService.getCurrentTimestamp()` causing timezone issues
- Database operations always wrote raw SQL instead of using `/mysql-exec` skill

**Solution**:
- ✅ Write recurring rules into CLAUDE.md "Critical Rules"
- ✅ Encapsulate common operations as Skills (like `/setup-java`, `/mysql-exec`)
- ✅ Use Hooks to enforce key constraints

#### Mistake 2: Too Much CLAUDE.md Content Causing Token Exhaustion

**Problem Manifestation**:
- Each session quickly prompts token insufficiency
- Need frequent `/clear` to restart
- Wait time for summary generation too long (1-2 minutes)

**Real Cases**:
- Initial CLAUDE.md contained lots of detailed example code (20KB+)
- Each read consumed 15K+ tokens
- 5-6 rounds of dialogue then needed session restart

**Solution**:
- ✅ Delete example code, keep only rules and principles
- ✅ Move detailed docs to external files (like `docs/api-design.md`)
- ✅ Keep CLAUDE.md within 5-10KB
- ✅ Use "ALWAYS/NEVER" format concise rules

#### Mistake 3: Too Large Requirement Steps Causing Frequent Database Changes

**Problem Manifestation**:
- Late-stage frequent database table structure modifications
- Each modification affects massive frontend/backend changes
- Data migration scripts increasingly complex

**Real Cases**:
- Version 1 expense management didn't consider multi-currency, later addition modified 5 tables
- Led to: 10+ APIs need modification, 8 frontend components need updates, 3 stored procedures need rewrite
- Data migration script: 300+ lines complex SQL (including data conversion and validation logic)
- Another example: Claude initially designed too many tables and fields ("might be useful" redundant design)
- Later cleanup work: Delete 4 unused tables, 20+ redundant fields, took 2 days

**Solution**:
- ✅ Use Planning Mode to fully design data model
- ✅ Only implement one small feature at a time, fully test before expanding
- ✅ Important fields (like currency) consider in version 1
- ✅ Reference existing module data model design

#### Mistake 4: Only Testing Functionality Not Reviewing Implementation Causing Technical Debt

**Problem Manifestation**:
- Surface functionality same, but backend implementation completely different
- Late-stage refactoring discovered lots of inconsistent code
- Hard to maintain and extend

**Real Cases**:
- Asset analysis and expense analysis pages look similar
- But backend: One uses stored procedure, one uses Java Service layer
- Data retrieval: One real-time query, one cached results
- Refactoring found can't unify optimization

**Solution**:
- ✅ After each feature complete, briefly review implementation code
- ✅ Check if follows existing architecture patterns
- ✅ Similar features should use similar implementation approaches
- ✅ In CLAUDE.md clearly state architecture decisions (like "aggregation uses stored procedures")

#### Mistake 5: Improper .gitignore Configuration Causing Sensitive Info Leaks

**Problem Manifestation**:
- Sensitive info (database passwords, API keys) committed to Git
- Claude exposes sensitive info in commit messages
- Need git filter-branch to clean history (complex and dangerous)

**Real Cases**:
- Initially didn't configure `.gitignore`, `backend/.env` file committed
- File contained: Database password, email server credentials
- Claude-generated commit message: "Add database config with password mysql123"
- After discovery needed: Delete historical commits, regenerate keys, force push

**Solution**:
- ✅ Configure `.gitignore` immediately at project initialization
- ✅ In CLAUDE.md clearly mark: "NEVER commit backend/.env"
- ✅ Use pre-commit hook to check sensitive files
- ✅ Regularly review commit messages, avoid exposing sensitive info
- ✅ Use `/git-commit-push` skill to auto-generate commit messages (can manually review)

**Core Lesson**:
> **Claude Code efficiency highly depends on good project management habits. Lazy early preparation will be repaid double in later stages.**

---

## Read the Full Series

- [Part 1: Introduction to Vibe Coding](MEDIUM_URL_PART_1)
- [Part 2: Claude Code Overview and Comparison](MEDIUM_URL_PART_2)
- [Part 3: Full Development Cycle Walkthrough](MEDIUM_URL_PART_3)
- [Part 4: AI-Era Software Methodologies](MEDIUM_URL_PART_4)
- [Part 5: Use Cases and Limitations](MEDIUM_URL_PART_5) (You are here)
- [Part 6: Lessons from 41k Lines of Code](MEDIUM_URL_PART_6)

---

**About the Author**: Austin Xu is a software engineer based in the Bay Area, specializing in cloud infrastructure and Kubernetes-based private cloud platforms. Outside of work, he's an avid tennis and pickleball player who has competed in numerous tournaments, including winning a USTA national championship with his team. Austin is passionate about AI-assisted development and actively organizes community events in the Bay Area focused on AI, personal finance, and leadership. This series documents his real-world experience building a 41,000-line family finance application in just 20 hours using Claude Code.

**Project**: https://github.com/austinxyz/finance
**Connect**: https://medium.com/@austin.xyz
