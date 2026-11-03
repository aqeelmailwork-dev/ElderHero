FROM node:22-trixie-slim
RUN apt-get update && apt-get install -y --no-install-recommends openjdk-21-jre-headless \
 && rm -rf /var/lib/apt/lists/*
RUN npm install -g firebase-tools
WORKDIR /srv
# Import saved data only if a previous export exists, always export on shutdown
CMD ["sh","-c","IMPORT=''; [ -f data/firebase-export-metadata.json ] && IMPORT='--import=data'; exec firebase emulators:start --project demo-elderhero $IMPORT --export-on-exit=data"]
