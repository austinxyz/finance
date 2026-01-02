const puppeteer = require('puppeteer');
const fs = require('fs');
const path = require('path');

// Table data definitions
const tables = {
  'part4-table-code-review': {
    title: 'Code Review Process Comparison',
    headers: ['Phase', 'Traditional', 'AI-Assisted'],
    rows: [
      ['First-round review', 'Human (1-2 days)', 'AI (5 minutes)'],
      ['Common issue detection rate', '70%', '90%'],
      ['Fix time', 'Human (1-2 hours)', 'AI (10 minutes)'],
      ['Final quality', 'Good', 'Good']
    ],
    filename: 'part4-table-code-review.png'
  },
  'part4-table-build-deploy': {
    title: 'Build & Deploy Efficiency Comparison',
    headers: ['Task', 'Traditional', 'Using Skills'],
    rows: [
      ['Build+push image', '5 minutes (multiple commands)', '30 seconds (`/docker-build-push`)'],
      ['Commit code', '2 minutes (3-4 commands)', '10 seconds (`/git-commit-push`)'],
      ['Database migration', '1 minute (look up credentials+execute)', '5 seconds (`/mysql-exec`)']
    ],
    filename: 'part4-table-build-deploy.png'
  },
  'part5-table-documentation': {
    title: 'Documentation and Test Writing Comparison',
    headers: ['Task', 'Traditional Time', 'AI Time', 'Quality Assessment'],
    rows: [
      ['Unit tests (30 Services)', '8 hours', '1.5 hours', '85%+ coverage'],
      ['Requirements doc sync (post-iteration)', '3 hours', '30 minutes', 'Needs manual review'],
      ['Design doc update', '2 hours', '20 minutes', 'Accurately reflects changes'],
      ['Database doc sync', '2 hours', '15 minutes', 'Complete and accurate'],
      ['Architecture diagrams (Mermaid)', '1 hour', '5 minutes', 'Clear and accurate']
    ],
    filename: 'part5-table-documentation.png'
  }
};

// HTML template generator
function generateHTML(tableData) {
  const { title, headers, rows } = tableData;

  return `
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>${title}</title>
  <style>
    * {
      margin: 0;
      padding: 0;
      box-sizing: border-box;
    }

    body {
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      padding: 60px;
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 100vh;
    }

    .container {
      background: white;
      border-radius: 16px;
      box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
      overflow: hidden;
      max-width: 1400px;
      width: 100%;
    }

    .header {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      padding: 32px 48px;
      color: white;
    }

    h1 {
      font-size: 32px;
      font-weight: 700;
      margin: 0;
      letter-spacing: -0.5px;
    }

    table {
      width: 100%;
      border-collapse: collapse;
    }

    thead tr {
      background: linear-gradient(135deg, #f5f7fa 0%, #e9ecef 100%);
    }

    th {
      padding: 24px 32px;
      text-align: left;
      font-weight: 700;
      font-size: 18px;
      color: #2d3748;
      border-bottom: 3px solid #667eea;
    }

    tbody tr {
      transition: background-color 0.2s ease;
    }

    tbody tr:nth-child(odd) {
      background-color: #ffffff;
    }

    tbody tr:nth-child(even) {
      background-color: #f8f9fa;
    }

    tbody tr:hover {
      background-color: #e9ecef;
    }

    td {
      padding: 20px 32px;
      font-size: 16px;
      color: #4a5568;
      border-bottom: 1px solid #e2e8f0;
      line-height: 1.6;
    }

    td code {
      background: #edf2f7;
      padding: 4px 8px;
      border-radius: 4px;
      font-family: 'Monaco', 'Courier New', monospace;
      font-size: 14px;
      color: #667eea;
    }

    tbody tr:last-child td {
      border-bottom: none;
    }

    .highlight {
      font-weight: 600;
      color: #667eea;
    }
  </style>
</head>
<body>
  <div class="container">
    <div class="header">
      <h1>${title}</h1>
    </div>
    <table>
      <thead>
        <tr>
          ${headers.map(h => `<th>${h}</th>`).join('')}
        </tr>
      </thead>
      <tbody>
        ${rows.map(row => `
          <tr>
            ${row.map((cell, idx) => {
              // Highlight improvements (shorter times, higher percentages)
              const isImprovement = idx > 0 && (
                cell.includes('minutes)') ||
                cell.includes('seconds') ||
                cell.includes('90%') ||
                cell.includes('85%+')
              );
              const className = isImprovement ? 'highlight' : '';
              return `<td class="${className}">${cell}</td>`;
            }).join('')}
          </tr>
        `).join('')}
      </tbody>
    </table>
  </div>
</body>
</html>
  `;
}

// Main function to generate all table images
async function generateTableImages() {
  const browser = await puppeteer.launch({
    headless: 'new',
    defaultViewport: {
      width: 2800,
      height: 2000,
      deviceScaleFactor: 2
    }
  });

  const page = await browser.newPage();
  const outputDir = path.join(__dirname, 'images');

  // Ensure output directory exists
  if (!fs.existsSync(outputDir)) {
    fs.mkdirSync(outputDir, { recursive: true });
  }

  for (const [key, tableData] of Object.entries(tables)) {
    console.log(`Generating ${tableData.filename}...`);

    // Generate HTML
    const html = generateHTML(tableData);

    // Set content
    await page.setContent(html, { waitUntil: 'load' });

    // Wait for rendering
    await new Promise(resolve => setTimeout(resolve, 1000));

    // Get container dimensions
    const container = await page.$('.container');
    const boundingBox = await container.boundingBox();

    // Take screenshot
    const outputPath = path.join(outputDir, tableData.filename);
    await page.screenshot({
      path: outputPath,
      clip: {
        x: boundingBox.x - 60,
        y: boundingBox.y - 60,
        width: boundingBox.width + 120,
        height: boundingBox.height + 120
      }
    });

    console.log(`✓ Generated ${tableData.filename}`);
  }

  await browser.close();
  console.log('\n✓ All table images generated successfully!');
}

// Run the script
generateTableImages().catch(error => {
  console.error('Error generating table images:', error);
  process.exit(1);
});
