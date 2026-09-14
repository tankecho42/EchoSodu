#!/usr/bin/env python3
"""Compile and run Android-independent regressions with the configured JDK."""
import os,pathlib,subprocess,shutil,json
root=pathlib.Path(__file__).resolve().parents[1]
java=pathlib.Path(os.environ.get('JAVA_HOME','/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home'))/'bin'
output=root/'artifacts/core-tests';output.mkdir(parents=True,exist_ok=True)
src=root/'android/app/src/main/java/com/tankecho/zensudoku'
classes=['Game','HintEngine','EffectTimeline','RegionMotion','VictoryMotion','TutorialLesson']
tests=['GameTest','RegionMotionTest','VictoryMotionTest','TutorialLessonTest']
subprocess.run([str(java/'javac'),'-encoding','UTF-8','-d',str(output),*[str(src/(c+'.java')) for c in classes],*[str(root/'tools'/(c+'.java')) for c in tests]],check=True)
bank=json.loads((root/'android/app/src/main/assets/puzzles.json').read_text())
fixtures=output/'puzzles.tsv';fixtures.write_text('\n'.join(p['puzzle']+'\t'+p['solution'] for level in bank for p in level))
for test in tests:subprocess.run([str(java/'java'),'-cp',str(output),test,*([str(fixtures)] if test=='GameTest' else [])],check=True)
