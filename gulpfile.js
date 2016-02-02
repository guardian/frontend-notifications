var gulp   = require('gulp');
var yaml = require('gulp-yaml');

//Cloudformation Tasks
gulp.task('buildCloudformation', function() {
  return gulp.src('./cloudformation/*.yml')
    .pipe(yaml({ schema: 'DEFAULT_SAFE_SCHEMA' }))
    .pipe(gulp.dest('./cloudformation'));
});
