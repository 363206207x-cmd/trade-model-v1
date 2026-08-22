import assert from "node:assert/strict";
import fs from "node:fs";
import vm from "node:vm";
import { spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";

const source = fs.readFileSync("src/main/resources/static/js/home-runtime.js", "utf8");

function extractFunction(name) {
    const start = source.indexOf(`function ${name}(`);
    assert.notEqual(start, -1, `${name} must exist in production home-runtime.js`);
    const open = source.indexOf("{", start);
    let depth = 0;
    for (let index = open; index < source.length; index += 1) {
        if (source[index] === "{") depth += 1;
        if (source[index] === "}") depth -= 1;
        if (depth === 0) return source.slice(start, index + 1);
    }
    throw new Error(`unterminated function ${name}`);
}

function formatter() {
    const context = { Intl, Date };
    vm.runInNewContext(`${extractFunction("has")}\n${extractFunction("clockTime")}\nthis.clockTime = clockTime;`, context);
    return context.clockTime;
}

if (process.argv.includes("--child")) {
    const clockTime = formatter();
    const timestamp = "2026-08-20T09:56:00Z";
    const result = {
        timezone: process.env.TZ,
        status: clockTime(timestamp),
        header: clockTime(timestamp),
        legacyWithoutOffset: clockTime("2026-08-20T09:56:00"),
        nullValue: clockTime(null),
        undefinedValue: clockTime(undefined),
        emptyValue: clockTime("")
    };
    process.stdout.write(JSON.stringify(result));
} else {
    assert.match(source, /clockTime\(header\.updatedAt\)/);
    assert.match(source, /clockTime\(state\.dataQuality\.value\)/);
    assert.equal((source.match(/function clockTime\(/g) || []).length, 1);

    const script = fileURLToPath(import.meta.url);
    function run(timezone) {
        const child = spawnSync(process.execPath, [script, "--child"], {
            encoding: "utf8",
            env: { ...process.env, TZ: timezone }
        });
        assert.equal(child.status, 0, child.stderr);
        return JSON.parse(child.stdout);
    }

    const utc = run("UTC");
    const shanghai = run("Asia/Shanghai");

    assert.equal(utc.status, "09:56");
    assert.equal(utc.header, "09:56");
    assert.equal(shanghai.status, "17:56");
    assert.equal(shanghai.header, "17:56");
    assert.equal(shanghai.legacyWithoutOffset, "09:56");
    assert.notEqual(shanghai.status, shanghai.legacyWithoutOffset);
    for (const result of [utc, shanghai]) {
        assert.equal(result.nullValue, "—");
        assert.equal(result.undefinedValue, "—");
        assert.equal(result.emptyValue, "—");
    }

    console.log("HOME_TIMESTAMP_TRANSPORT_MATRIX=PASS");
    console.log("UTC_STATUS=09:56 UTC_HEADER=09:56");
    console.log("ASIA_SHANGHAI_STATUS=17:56 ASIA_SHANGHAI_HEADER=17:56");
    console.log("LEGACY_NO_OFFSET_ASIA_SHANGHAI=09:56");
    console.log("NULL_TIMESTAMP=—");
}
