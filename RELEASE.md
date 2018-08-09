# Manual release process

1 - seal the version by removing -SNAPSHOT from `version.sbt` and push the change
2 - tag the sealed version

```
git tag x.y.z
git push origin x.y.z
```

3 - publish the site

```
sbt docs/clean
sbt docs/makeSite
sbt docs/ghpagesPushSite
```

4 - sign and publish the artifacts to Sonatype

```
sbt +publishSigned
```

5 - release the staging repositories

```
sbt sonatypeRelease 
```

(or via UI at https://oss.sonatype.org/#stagingRepositories)

6 - start next version development phase by bumping version and putting back -SNAPSHOT in `version.sbt`