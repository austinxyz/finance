# Medium Article Table Images

This directory contains professional PNG images for all tables in the Medium article series.

## Generated Images

### Part 2: Claude Code Overview

1. **part2-table-core-concepts.png** (2800 x 1132)
   - Claude Code Core Concepts
   - 7 core concepts with purposes, analogies, and documentation links
   - Used in: Part 2, after "Core Concepts Overview" section

2. **part2-table-tool-comparison.png** (2800 x 1292)
   - AI Development Tools Comparison
   - Compares Claude Code, Cursor, Cline, GitHub Copilot, and Gemini CLI
   - Features: Code Generation, Command Execution, Context Understanding, etc.
   - Used in: Part 2, "Tool Comparison: Claude Code vs. Competitors" section

### Part 3: Development Walkthrough

3. **part3-table-architecture-comparison.png** (2800 x 708)
   - Architecture Implementation Approaches
   - Compares Stored Procedure, Java Service, and Frontend Calculation
   - Analyzes Performance, Maintainability, Testing Difficulty, Use Case
   - Used in: Part 3, "Architecture Trade-offs" section

4. **part3-table-skills-vs-manual.png** (2800 x 800)
   - Skills vs Manual Commands
   - Compares manual database operations vs. /mysql-exec Skill
   - Metrics: Time Cost, Password Leak Risk, Repeatability, AI Usability
   - Used in: Part 3, "The Power of Skills" section

### Part 4: Methodology Evolution

5. **part4-table-code-review.png** (2800 x ~700)
   - Code Review Process Comparison
   - Compares Traditional vs AI-Assisted code review workflow
   - Metrics: First-round review time, Detection rate, Fix time, Final quality
   - Used in: Part 4, "Code Review: New Balance of Human-AI Collaboration" section

6. **part4-table-build-deploy.png** (2800 x ~700)
   - Build & Deploy Efficiency Comparison
   - Compares Traditional multi-step commands vs Skills workflow
   - Tasks: Build+push image, Commit code, Database migration
   - Used in: Part 4, "Build & Deploy: Simplified Automation Processes" section

### Part 5: Use Cases and Limitations

7. **part5-table-documentation.png** (2800 x ~900)
   - Documentation and Test Writing Comparison
   - Compares Traditional vs AI-assisted time and quality
   - Tasks: Unit tests, Requirement docs, Design docs, Database docs, Architecture diagrams
   - Used in: Part 5, "Documentation and Test Writing" section

## Design Features

All images feature:
- Professional gradient design (purple/blue theme)
- Clean, modern typography
- Responsive layout (1400px max width)
- High resolution (2800px width, 2x device scale factor)
- Alternating row colors for readability
- Star ratings (⭐) for visual clarity
- Consistent branding across all tables

## How to Use in Medium

1. Upload images to Medium article
2. Insert at appropriate sections
3. Add alt text for accessibility:
   - Table 1: "Claude Code Core Concepts Overview"
   - Table 2: "AI Development Tools Feature Comparison"
   - Table 3: "Architecture Implementation Approaches Comparison"
   - Table 4: "Skills vs Manual Commands Comparison"
   - Table 5: "Code Review Process Comparison: Traditional vs AI-Assisted"
   - Table 6: "Build & Deploy Efficiency Comparison"
   - Table 7: "Documentation and Test Writing Time Comparison"

## Technical Details

- Format: PNG
- Resolution: 2800px width (retina-ready)
- Color depth: 8-bit RGB
- File sizes: 248KB - 501KB
- Generated using: HTML + Puppeteer (headless Chrome)

## Regenerating Images

To regenerate all table images:

```bash
cd docs/medium-ready
npm install  # First time only
node generate-table.js
```

The script will generate all table images in the `images/` directory.
