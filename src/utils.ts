import * as vscode from 'vscode';
import * as fs from 'fs';
const _ = require('lodash');
const path = require('path');
const fsx = require('fs-extra');
const yaml = require('js-yaml');

const GLOBAL_CONFIG = '.jenkins-jack.config.yaml'
function _sleep(ms: number) {
    return new Promise((resolve) => setTimeout(resolve, ms));
}

export function findConfig(aScriptPath: string) {
    let currentDir = path.dirname(aScriptPath)
    let dirs = [currentDir]

    let count = 100
    while ((path.dirname(currentDir) != currentDir) && count > 0 ) {
        count -= 1
        currentDir = path.dirname(currentDir)
        dirs.push(currentDir)
    }
    
    dirs = dirs.sort().reverse()
    
    let foundGConfig = false
    let gConfigFile : string = '';
    _.each(dirs, (dir: string) => {
        let p = path.join(dir, GLOBAL_CONFIG)
        if (!foundGConfig) {
            console.log('#find gconfig : ', p)
        }
        if (foundGConfig) {
            console.log('  #found gconfig : --- ', true , ' ---')
        } else {
            foundGConfig = fsx.existsSync(p)
            if (foundGConfig) {
                gConfigFile = p
            }
        }
    })

    let gConfig = {}
    if (foundGConfig) {
        gConfig = yaml.safeLoad(fs.readFileSync(gConfigFile), 'utf-8')
    }

    let groovyScriptPath = aScriptPath;
    let groovyConfigPath = null
    const isConfig = groovyScriptPath.match(/^.*\/(.*).config.yaml$/)
    const isGroovy = groovyScriptPath.match(/^.*\/(.*).groovy$/)
    let scriptName = ''

    let jobConfig = { name: scriptName, params: null }
    
    if (isConfig) {
        scriptName = isConfig[1];
        groovyConfigPath = path.join(path.dirname(groovyScriptPath), `${scriptName}.config.yaml`)
        groovyScriptPath = path.join(path.dirname(groovyScriptPath), `${scriptName}.groovy`)
    } else if (isGroovy) {
        scriptName = isGroovy[1];
        groovyConfigPath = path.join(path.dirname(groovyScriptPath), `${scriptName}.config.yaml`)
        groovyScriptPath = path.join(path.dirname(groovyScriptPath), `${scriptName}.groovy`)
    }

    if (fsx.existsSync(groovyConfigPath)) {
        jobConfig = yaml.safeLoad(fs.readFileSync(groovyConfigPath), 'utf-8')
        if (!_.isString(jobConfig.name)) {
            jobConfig.name = scriptName
        }
    } else if (foundGConfig) {
        let nameFullPath = groovyScriptPath.toString().replace(path.dirname(gConfigFile).toString(), '')
        nameFullPath = nameFullPath.split(path.sep).join('/').replace('.groovy', '')
        if (nameFullPath.startsWith(path.sep)) {
            nameFullPath = nameFullPath.substr(path.sep.toString().length)
        }
        console.log('---nameFullPath---> ', nameFullPath)
        jobConfig.name = nameFullPath
    }
    
    const self = {
        gConfig, groovyConfigPath, groovyScriptPath, jobConfig,
        getJobName: (name: string) => {
            let ret = name
            const jobPrefix = _.get(self, 'gConfig.job.prefix');
            
            if (_.isString(jobPrefix)) {
                if (jobPrefix.endsWith('/')) {
                    ret = `${jobPrefix}${name}`
                } else {
                    ret = `${jobPrefix}/${name}`
                }
            }
            return ret
        }
    }
    return self
}

/**
 * Async sleep utility method.
 * @param ms Milliseconds to sleep.
 */
export async function sleep(ms: number) {
    await _sleep(ms);
}

export function isGroovy() {
    let retVal = false
    var editor = vscode.window.activeTextEditor;
    if (!editor) { return false; }
    // retVal = ("groovy" === editor.document.languageId);
    retVal = ['groovy', 'json', 'yaml', 'yml'].includes(editor.document.languageId);
    return retVal
}

export async function showQuicPick(items: any[], ): Promise<void> {
    let qp = vscode.window.createQuickPick();
    qp.items = items;
    qp.title = ''

}

/**
 * Utility for parsing a json file and returning
 * its contents.
 * @param path The path to the json file.
 * @returns The parsed json.
 */
export function readjson(path: string): any {
    let raw: any;
    let json: any;
    try {
        if (path.endsWith('.yaml') || path.endsWith('.yml')) {
            json  = yaml.safeLoad(fs.readFileSync(path))
        } else {
            raw  = fs.readFileSync(path);
            json = JSON.parse(raw);
        }
    } catch (err) {
        console.log(err)
        err.message = `Could not parse parameter JSON from ${path}`;
        throw err;
    }
    return json;
}

/**
 * Writes the given json to disk.
 * @param path The the file path (file included) to write to.
 * @param json The json to write out.
 */
export function writejson(path: string, json: any) {
    if (_.isEmpty(json.params)) {
        console.log('Skip: WriteJson --> ', json)
        return
    }
    console.log('WriteJson --> ', json)
    try {
        let jsonString = JSON.stringify(json, null, 4);
        // fs.writeFileSync(path, jsonString, 'utf8');
        fs.writeFileSync(path, yaml.dump(JSON.parse(jsonString)), 'utf8');
    } catch (err) {
        err.message = `Could not write parameter JSON to ${path}`;
        throw err;
    }
}

/**
 * TODO: HACK
 * Returns some nasty hard-coded Jenkins Pipeline
 * XML as a Pipeline job config template.
 */
export function getPipelineJobConfig() {
    return `<?xml version="1.0" encoding="UTF-8"?>
<flow-definition plugin="workflow-job@2.10">
    <description />
    <keepDependencies>false</keepDependencies>
    <properties>
        <com.sonyericsson.rebuild.RebuildSettings plugin="rebuild@1.25">
            <autoRebuild>false</autoRebuild>
            <rebuildDisabled>false</rebuildDisabled>
        </com.sonyericsson.rebuild.RebuildSettings>
        <com.synopsys.arc.jenkinsci.plugins.jobrestrictions.jobs.JobRestrictionProperty plugin="job-restrictions@0.4" />
        <hudson.plugins.throttleconcurrents.ThrottleJobProperty plugin="throttle-concurrents@2.0">
            <categories class="java.util.concurrent.CopyOnWriteArrayList" />
            <throttleEnabled>false</throttleEnabled>
            <throttleOption>project</throttleOption>
            <limitOneJobWithMatchingParams>false</limitOneJobWithMatchingParams>
            <paramsToUseForLimit />
        </hudson.plugins.throttleconcurrents.ThrottleJobProperty>
        <org.jenkinsci.plugins.workflow.job.properties.PipelineTriggersJobProperty>
            <triggers />
        </org.jenkinsci.plugins.workflow.job.properties.PipelineTriggersJobProperty>
    </properties>
    <definition class="org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition" plugin="workflow-cps@2.29">
        <script></script>
        <sandbox>false</sandbox>
    </definition>
    <triggers />
</flow-definition>`;
}
