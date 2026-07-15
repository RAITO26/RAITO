const https = require('https');
const fs = require('fs');
const path = require('path');

const altUrls = [
    'https://raw.githubusercontent.com/Dehghan/Persian-Fonts/master/fonts/B_Nazanin.ttf',
    'https://raw.githubusercontent.com/vahidreza/farsi-direct-links/master/B-Nazanin.ttf',
    'https://raw.githubusercontent.com/ictis/ICTIS-Portal/master/fonts/B-Nazanin.ttf',
    'https://raw.githubusercontent.com/SahandAndisheh/AndishehFont/master/Fonts/B-Nazanin.ttf',
    'https://raw.githubusercontent.com/mahdiy2/Farsi_Fonts/master/B-Nazanin.ttf',
    'https://raw.githubusercontent.com/Dehghan/Persian-Fonts/master/B_Nazanin.ttf',
    'https://raw.githubusercontent.com/morteza/persian-fonts/master/B-Nazanin.ttf',
    'https://cdn.jsdelivr.net/gh/vahidreza/farsi-direct-links@master/B-Nazanin.ttf',
    'https://cdn.jsdelivr.net/gh/Dehghan/Persian-Fonts@master/fonts/B_Nazanin.ttf'
];

const destDir = '/app/src/main/res/font';
const destFile = path.join(destDir, 'b_nazanin.ttf');

if (!fs.existsSync(destDir)) {
    fs.mkdirSync(destDir, { recursive: true });
}

function fetchUrl(url) {
    return new Promise((resolve, reject) => {
        console.log(`Trying URL: ${url}`);
        const request = https.get(url, (response) => {
            if (response.statusCode === 301 || response.statusCode === 302 || response.statusCode === 307 || response.statusCode === 308) {
                // follow redirect
                const redirectUrl = response.headers.location;
                console.log(`Redirecting to: ${redirectUrl}`);
                https.get(redirectUrl, (redResponse) => {
                    handleResponse(redResponse, resolve, reject);
                }).on('error', reject);
            } else {
                handleResponse(response, resolve, reject);
            }
        });
        request.on('error', (err) => {
            reject(err);
        });
    });
}

function handleResponse(response, resolve, reject) {
    if (response.statusCode !== 200) {
        reject(new Error(`Status: ${response.statusCode}`));
        return;
    }
    const data = [];
    response.on('data', (chunk) => {
        data.push(chunk);
    });
    response.on('end', () => {
        const buffer = Buffer.concat(data);
        resolve(buffer);
    });
}

async function start() {
    let success = false;
    for (const url of altUrls) {
        try {
            const buffer = await fetchUrl(url);
            fs.writeFileSync(destFile, buffer);
            console.log(`🏆 SUCCESS: Successfully downloaded and written to: ${destFile} (${buffer.length} bytes)`);
            success = true;
            break;
        } catch (err) {
            console.log(`❌ FAILED for URL ${url}: ${err.message}`);
        }
    }
    if (!success) {
        console.log('🔴 Absolute failure: Could not download the font file from any of the URLs.');
        process.exit(1);
    }
}

start();
