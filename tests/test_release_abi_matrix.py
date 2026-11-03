"""Both release jobs must package and verify their own architecture."""
import os,subprocess,tempfile,unittest,zipfile,json,re
from pathlib import Path
from unittest.mock import patch
import yaml
WORKFLOW=Path(__file__).resolve().parents[1]/'.github/workflows/personal-apk.yml'
class AbiMatrixTests(unittest.TestCase):
 def test_two_architectures_have_distinct_artifacts_and_native_caches(self):
  build=yaml.safe_load(WORKFLOW.read_text())['jobs']['build']
  expression=build['strategy']['matrix']['include']
  variants=[json.loads(value) for value in re.findall(r"'(\[.*?\])'",expression)]
  self.assertEqual([[entry['abi'] for entry in variant] for variant in variants],[['arm64-v8a'],['x86_64'],['arm64-v8a','x86_64']])
  self.assertIn("inputs.architecture == 'arm64-v8a'",expression)
  self.assertIn("inputs.architecture == 'x86_64'",expression)
  matrix=variants[-1]
  self.assertEqual({x['abi'] for x in matrix},{'arm64-v8a','x86_64'})
  self.assertEqual(len({x['label'] for x in matrix}),2)
  self.assertFalse(build['strategy']['fail-fast'])
  self.assertEqual(build['env']['NATIVE_TARGET'],'${{ matrix.abi }}')
  for step in build['steps']:
   if step.get('name')=='Restore native compiler cache':self.assertIn('${{ matrix.abi }}',step['with']['key'])
   if step.get('name')=='Determine native inputs and runner memory':self.assertIn("os.environ['NATIVE_TARGET']",step['run'])
   if step.get('name')=='Upload APK only':self.assertIn('${{ matrix.label }}',step['with']['name'])

 def test_actual_verifier_accepts_each_matching_abi_and_rejects_cross_packaging(self):
  build=yaml.safe_load(WORKFLOW.read_text())['jobs']['build']
  step=next(s for s in build['steps'] if s.get('name')=='Verify APK contents, manifest and signature')
  script=step['run'];a=script.index('          apks =') if '          apks =' in script else script.index('apks =')
  b=script.index('    forbidden_paths',a)
  # Keep the actual package/ABI verifier, stopping before unrelated plugin/signing checks.
  script=script[a:b]
  for target in ['arm64-v8a','x86_64']:
   for packaged in [target,'wrong-abi']:
    with tempfile.TemporaryDirectory() as directory:
     base=Path(directory);apkdir=base/'TMessagesProj/build/outputs/apk/release';apkdir.mkdir(parents=True);(base/'artifact').mkdir()
     with zipfile.ZipFile(apkdir/'app.apk','w') as archive:archive.writestr(f'lib/{packaged}/libtmessages.49.so',b'fake-library-for-test')
     environment={'NATIVE_TARGET':target,'OPTIMIZATION_MODE':'release','APK_ABI_LABEL':target,'ANDROID_HOME':directory,'PERSONAL_APPLICATION_ID':'com.freezgram.app','PERSONAL_BUILD_LABEL':'Freezgram','PERSONAL_VERSION_CODE':'123'}
     manifest="package: name='com.freezgram.app' versionCode='123'\napplication-label:'Freezgram'\nnative-code: '"+target+"'"
     previous=os.getcwd()
     try:
      os.chdir(base)
      with patch.dict(os.environ,environment),patch.object(subprocess,'check_output',side_effect=lambda args,**kwargs: manifest if 'badging' in args else 'ru-rRU\nuk-rUA\n'):
       scope={'Path':Path,'os':os,'shutil':__import__('shutil'),'subprocess':subprocess,'zipfile':zipfile}
       if packaged==target:exec(script,scope)
       else:
        with self.assertRaises(SystemExit):exec(script,scope)
     finally:os.chdir(previous)
