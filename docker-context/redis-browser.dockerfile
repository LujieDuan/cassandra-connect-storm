FROM ruby:2.6.2-stretch

RUN apt-get update \
  && apt-get install -y nodejs \
  && gem install redis-browser \
  && rm -rf /var/lib/apt/lists/*

ENTRYPOINT [ "redis-browser" ]