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
    const context = { Intl, Date, window: {} };
    const shared = source.slice(0, source.indexOf("/* Desktop Home runtime */"));
    assert.ok(shared.includes("window.TrineDesktopSemantics"));
    vm.runInNewContext(`${shared}\nvar desktop = window.TrineDesktopSemantics;\n${extractFunction("clockTime")}\nthis.clockTime = clockTime;`, context);
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
    const newYork = run("America/New_York");

    for (const result of [utc, shanghai, newYork]) {
        assert.equal(result.status, "17:56");
        assert.equal(result.header, "17:56");
        assert.equal(result.legacyWithoutOffset, "17:56");
        assert.equal(result.nullValue, "尚无记录");
        assert.equal(result.undefinedValue, "尚无记录");
        assert.equal(result.emptyValue, "尚无记录");
    }

    console.log("HOME_TIMESTAMP_TRANSPORT_MATRIX=PASS");
    console.log("UTC_STATUS=17:56 UTC_HEADER=17:56");
    console.log("ASIA_SHANGHAI_STATUS=17:56 ASIA_SHANGHAI_HEADER=17:56");
    console.log("AMERICA_NEW_YORK_STATUS=17:56 AMERICA_NEW_YORK_HEADER=17:56");
    console.log("LEGACY_NO_OFFSET_ASIA_SHANGHAI=17:56");
    console.log("NULL_TIMESTAMP=尚无记录");
}
