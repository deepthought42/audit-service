# Look-see Audit Service

A comprehensive web auditing service that performs automated analysis of websites for accessibility, content quality, information architecture, and design system compliance. The service provides detailed audits at both page and domain levels, helping organizations ensure their web presence meets best practices and standards.

## Features

### Audit Categories
- **Accessibility Audits**: Evaluates web pages for WCAG compliance and accessibility best practices
- **Content Audits**: Analyzes content quality, readability, and metadata
- **Information Architecture Audits**: Reviews site structure, navigation, and link quality
- **Design System Audits**: Ensures consistent design implementation across pages

### Key Capabilities
- **Page-level Audits**: Detailed analysis of individual web pages
- **Domain-level Audits**: Comprehensive evaluation across entire websites
- **Real-time Progress Updates**: Live audit progress tracking via Pusher integration
- **Detailed Issue Reporting**: Identifies specific issues with explanations and recommendations
- **Scoring System**: Quantifies audit results with points-based scoring
- **Neo4j Database**: Graph database storage for complex audit relationships

### Specific Audit Types
- **Metadata Audits**: Evaluates page titles, descriptions, and meta tags
- **Readability Audits**: Analyzes content reading level and comprehension
- **Image Audits**: Checks image accessibility, copyright compliance, and optimization
- **Paragraphing Audits**: Reviews content structure and formatting
- **Link Audits**: Validates link functionality and accessibility

## Technical Stack
- Java Spring Boot
- Neo4j Graph Database
- Pusher for Real-time Updates
- Google Cloud NLP (for content analysis)
- Jsoup (for HTML parsing)

## Getting Started

### Prerequisites
- Java 11 or higher
- Neo4j Database
- Pusher Account (for real-time updates)
- Google Cloud credentials (for NLP features)

### Configuration
1. Set up your Neo4j database
2. Configure Pusher credentials in `application.properties`:
   ```
   pusher.appId=your_app_id
   pusher.key=your_key
   pusher.secret=your_secret
   pusher.cluster=your_cluster
   ```
3. Configure Google Cloud credentials for NLP features

### Running the Service
1. Build the project:
   ```bash
   ./gradlew build
   ```
2. Run the service:
   ```bash
   java -jar build/libs/audit-service.jar
   ```

## API Usage

### Starting an Audit
```http
POST /audit
Content-Type: application/json

{
    "url": "https://example.com",
    "auditLevel": "DOMAIN",
    "auditCategories": ["ACCESSIBILITY", "CONTENT", "INFORMATION_ARCHITECTURE"]
}
```

### Getting Audit Results
```http
GET /audit/{auditId}
```

### Real-time Updates
The service provides real-time audit progress updates through Pusher channels. Subscribe to the appropriate channel to receive live updates during audits.

## Audit Results

Audit results include:
- Overall scores for each audit category
- Detailed issue reports with:
  - Issue description
  - Priority level
  - Recommendations for resolution
  - Affected elements
- Progress tracking
- Historical audit data

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

[Add your license information here]

## GitHub Packages Authentication

This project depends on the `com.looksee:core` package from GitHub Packages. To resolve this dependency, you need to set up authentication.

### Local Development

#### Install LookseeCore JAR to Local Maven Repository

Before building, you must install the LookseeCore JAR to your local Maven repository:

```bash
mvn install:install-file -Dfile=libs/core-0.3.8.jar -DgroupId=com.looksee -DartifactId=core -Dversion=0.3.8 -Dpackaging=jar
```

1. Create a Personal Access Token (PAT) with the following permissions:
   - `read:packages` - to download packages
   - `repo` - if the package is in a private repository

2. Set environment variables:
   ```bash
   export GITHUB_TOKEN=your_personal_access_token
   export GITHUB_USERNAME=your_github_username
   ```

3. Run Maven with the settings file:
   ```bash
   mvn clean install --settings settings.xml
   ```

### For GitHub Actions

The workflow is configured to use the built-in `GITHUB_TOKEN` which should have access to packages in the same organization/repository.

### Troubleshooting

If you get a 401 Unauthorized error:

1. Ensure your PAT has the correct permissions
2. Verify the package exists at: https://maven.pkg.github.com/deepthought42/LookseeCore
3. Check that the package coordinates match: `com.looksee:core:0.0.3`
4. Make sure you're using the settings.xml file when running Maven commands

### Alternative: Use GitHub CLI

You can also authenticate using GitHub CLI:
```bash
gh auth login
mvn clean install --settings settings.xml
```