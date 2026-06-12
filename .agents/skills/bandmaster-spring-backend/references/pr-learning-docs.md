# PR Learning Documentation Reference

Use this when creating or updating BandMaster PR documentation in Notion or repository docs.

## Purpose

PR documentation is learning material for the user.

It should help the user understand the concepts used in the PR without needing to immediately search external articles.

Do not make the document a changelog. GitHub already preserves commits, changed files, and review history.

## Default Shape

Use this order unless the PR needs a better local structure.

1. What this document teaches
2. Core concept summary
3. Browser/client flow
4. Server/Spring flow
5. External tools and framework roles
6. How this PR implemented the idea
7. Key code snippets
8. Security and operational cautions
9. Tests and verification
10. Review lessons
11. References
12. AI handoff notes

## Writing Style

Prefer bullet-oriented writing over long paragraphs.

- Keep one paragraph to at most 2-3 lines.
- Split long explanations into bullets.
- Use numbered lists for flows.
- Keep bullets to one line when possible, and at most two lines.
- Do not stop at definitions; connect each concept to the current PR code.
- Keep implementation logs and changed-file lists out of the main learning section.

## Inline Code In Notion

Use Notion Command + E only for important keywords.

Use it for:

- Core concepts that should stand out
- Important class or method names
- Important error codes
- Configuration keys

Avoid it for:

- Repeated class names
- Ordinary file names
- Normal prose
- Section titles

## External Tools And Frameworks

When the PR relies on important external tools or frameworks, explain them explicitly.

Examples:

- Spring Security
- JPA
- Redis
- Bean Validation
- Docker
- MySQL

For each tool, explain:

- What responsibility it has in this PR
- How it processes requests or data internally
- Which extension point, configuration, or API the code uses
- What caution follows from using that tool

For Spring Security authentication work, usually cover:

- SecurityFilterChain
- FilterChainProxy
- OncePerRequestFilter when a custom filter is used
- SecurityContext and Authentication
- The difference between permitAll and skipping a filter

## Code Snippets

Include short snippets that show how the concept is implemented.

- Prefer 5-15 lines per snippet.
- Pick only the core lines that reveal the concept.
- Do not paste full files or repetitive DTO/getter code.
- Add 1-2 bullets below each snippet explaining what concept the code implements.

Good snippet categories:

- SecurityContext population
- Repository query or persistence boundary
- Transactional state change
- Redis atomic operation
- Cookie or CORS security setting
- Exception mapping
- Validation boundary

## Review Updates

When reflecting review comments, explain the lesson.

Include:

- What the review pointed out
- Why it was a real risk
- Which request/data flow caused the problem
- How the fix changed the flow
- What test or verification supports it
- What rule should be remembered next time

## References

Prefer official documentation first.

For each reference, include:

- Which concept it explains
- Why it matters in this PR
- Where the concept appears in the code

Use Korean blog posts when they help explain concepts in a way official docs do not.

Do not add references that are only loosely related to the implementation.
